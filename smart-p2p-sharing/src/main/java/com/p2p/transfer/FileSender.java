package com.p2p.transfer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.p2p.network.AckType;
import com.p2p.network.Packet;
import com.p2p.network.PacketType;
import com.p2p.network.UDPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class FileSender {

    private static final Logger LOGGER = LogManager.getLogger(FileSender.class);

    private final String peerId;
    private final int chunkSize;
    private final Duration ackTimeout;
    private final int maxRetries;
    private final Gson gson = new Gson();

    public FileSender(String peerId, int chunkSize, Duration ackTimeout, int maxRetries) {
        this.peerId = Objects.requireNonNull(peerId, "peerId");
        this.chunkSize = chunkSize;
        this.ackTimeout = ackTimeout;
        this.maxRetries = maxRetries;
    }

    public void sendFile(Path file, InetSocketAddress target) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(target, "target");
        UUID sessionId = UUID.randomUUID();
        String checksum = ChecksumUtil.sha256(file);
        try (UDPClient client = new UDPClient(); FileChunker chunker = new FileChunker(file, chunkSize)) {
            JsonObject metadata = new JsonObject();
            metadata.addProperty("sessionId", sessionId.toString());
            metadata.addProperty("fileName", file.getFileName().toString());
            metadata.addProperty("fileSize", chunker.getFileSize());
            metadata.addProperty("totalChunks", chunker.getTotalChunks());
            metadata.addProperty("checksum", checksum);
            metadata.addProperty("senderPeerId", peerId);
            metadata.addProperty("timestamp", Instant.now().toEpochMilli());

            sendWithRetry(client, Packet.metadata(sessionId, gson.toJson(metadata).getBytes()), target, AckType.METADATA, -1);
            LOGGER.info("Metadata acknowledged for session {} ({} bytes)", sessionId, chunker.getFileSize());

            for (FileChunker.Chunk chunk : chunker) {
                boolean delivered = false;
                int attempts = 0;
                while (!delivered && attempts <= maxRetries) {
                    attempts++;
                    Packet dataPacket = Packet.data(sessionId, chunk.getIndex(), chunk.getTotalChunks(), chunk.getData());
                    client.send(dataPacket, target);
                    try {
                        Packet ack = awaitAck(client, sessionId);
                        if (ack.getAckType().orElse(AckType.RETRY) == AckType.CHUNK && ack.getChunkId() == chunk.getIndex()) {
                            delivered = true;
                            LOGGER.debug("Chunk {} of {} acknowledged", chunk.getIndex(), chunk.getTotalChunks());
                        } else if (ack.getAckType().orElse(AckType.RETRY) == AckType.RETRY) {
                            LOGGER.warn("Chunk {} requested retry: {}", chunk.getIndex(), new String(ack.getPayload(), StandardCharsets.UTF_8));
                        } else if (ack.getAckType().orElse(AckType.RETRY) == AckType.REJECTED) {
                            throw new IOException("Transfer rejected by receiver: " + new String(ack.getPayload(), StandardCharsets.UTF_8));
                        }
                    } catch (SocketTimeoutException ex) {
                        LOGGER.warn("Timeout waiting for ack on chunk {} (attempt {}/{})", chunk.getIndex(), attempts, maxRetries);
                    }
                }
                if (!delivered) {
                    throw new IOException("Failed to deliver chunk " + chunk.getIndex() + " after " + maxRetries + " retries");
                }
            }

            LOGGER.info("All chunks sent for session {}. Awaiting completion ACK", sessionId);
            Packet completionAck = awaitAck(client, sessionId);
            if (completionAck.getAckType().orElse(AckType.RETRY) != AckType.COMPLETE) {
                throw new IOException("Unexpected completion acknowledgement: " + completionAck);
            }
            LOGGER.info("Transfer session {} completed successfully", sessionId);
        }
    }

    private void sendWithRetry(UDPClient client, Packet packet, InetSocketAddress target, AckType expectedAck, int chunkId) throws IOException {
        int attempts = 0;
        while (attempts <= maxRetries) {
            attempts++;
            client.send(packet, target);
            try {
                Packet ack = awaitAck(client, packet.getSessionId());
                if (ack.getAckType().orElse(AckType.RETRY) == expectedAck && (chunkId < 0 || ack.getChunkId() == chunkId)) {
                    return;
                }
                if (ack.getAckType().orElse(AckType.RETRY) == AckType.REJECTED) {
                    throw new IOException("Transfer rejected by receiver: " + new String(ack.getPayload(), StandardCharsets.UTF_8));
                }
            } catch (SocketTimeoutException ex) {
                LOGGER.warn("Timeout waiting for {} ack (attempt {}/{})", expectedAck, attempts, maxRetries);
            }
        }
        throw new IOException("Failed to obtain " + expectedAck + " acknowledgement after " + maxRetries + " retries");
    }

    private Packet awaitAck(UDPClient client, UUID sessionId) throws IOException {
        long deadline = System.nanoTime() + ackTimeout.toNanos();
        while (System.nanoTime() < deadline) {
            Duration remaining = Duration.ofNanos(Math.max(0, deadline - System.nanoTime()));
            try {
                Packet packet = client.receive(remaining);
                if (packet.getPacketType() != PacketType.ACK) {
                    continue;
                }
                if (!packet.getSessionId().equals(sessionId)) {
                    LOGGER.debug("Ignoring ACK for other session {}", packet.getSessionId());
                    continue;
                }
                return packet;
            } catch (SocketTimeoutException ex) {
                throw ex;
            }
        }
        throw new SocketTimeoutException("Timed out waiting for ACK for session " + sessionId);
    }
}
