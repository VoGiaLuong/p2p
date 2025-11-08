package com.p2p.transfer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.p2p.network.AckType;
import com.p2p.network.Packet;
import com.p2p.network.PacketType;
import com.p2p.network.UDPServer;
import com.p2p.security.SecurityChecker;
import com.p2p.security.SecurityChecker.SecurityResult;
import com.p2p.storage.StorageManager;
import com.p2p.webhook.N8nClient;
import com.p2p.webhook.WebhookPayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FileReceiver implements UDPServer.PacketHandler {

    private static final Logger LOGGER = LogManager.getLogger(FileReceiver.class);

    private final StorageManager storageManager;
    private final SecurityChecker securityChecker;
    private final N8nClient n8nClient;
    private final Map<UUID, TransferSession> sessions = new ConcurrentHashMap<>();

    public FileReceiver(StorageManager storageManager, SecurityChecker securityChecker, N8nClient n8nClient) {
        this.storageManager = storageManager;
        this.securityChecker = securityChecker;
        this.n8nClient = n8nClient;
    }

    @Override
    public void handle(Packet packet, InetAddress address, int port, DatagramSocket socket) {
        try {
            if (packet.getPacketType() == PacketType.METADATA) {
                handleMetadata(packet, address, port, socket);
            } else if (packet.getPacketType() == PacketType.DATA) {
                handleData(packet, address, port, socket);
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to process packet {}", packet, ex);
            sendAck(socket, packet.getSessionId(), AckType.RETRY, packet.getChunkId(), ex.getMessage(), address, port);
        }
    }

    private void handleMetadata(Packet packet, InetAddress address, int port, DatagramSocket socket) throws IOException {
        JsonObject metadata = JsonParser.parseString(new String(packet.getPayload(), StandardCharsets.UTF_8)).getAsJsonObject();
        UUID sessionId = packet.getSessionId();
        TransferSession session = new TransferSession(sessionId,
                metadata.get("fileName").getAsString(),
                metadata.get("checksum").getAsString(),
                metadata.get("totalChunks").getAsInt(),
                metadata.get("fileSize").getAsLong(),
                metadata.get("senderPeerId").getAsString(),
                storageManager.createSessionTempDirectory(sessionId));
        sessions.put(sessionId, session);
        LOGGER.info("Metadata received for session {} from {}:{} -> {} ({} bytes, {} chunks)",
                sessionId, address.getHostAddress(), port, session.fileName, session.fileSize, session.totalChunks);
        sendAck(socket, sessionId, AckType.METADATA, -1, "Metadata accepted", address, port);
    }

    private void handleData(Packet packet, InetAddress address, int port, DatagramSocket socket) throws IOException {
        TransferSession session = sessions.get(packet.getSessionId());
        if (session == null) {
            LOGGER.warn("Received chunk for unknown session {}", packet.getSessionId());
            sendAck(socket, packet.getSessionId(), AckType.RETRY, packet.getChunkId(), "Unknown session", address, port);
            return;
        }
        int chunkId = packet.getChunkId();
        if (chunkId < 0 || chunkId >= session.totalChunks) {
            LOGGER.warn("Received invalid chunk id {} for session {}", chunkId, session.sessionId);
            sendAck(socket, session.sessionId, AckType.RETRY, chunkId, "Invalid chunk id", address, port);
            return;
        }
        if (session.chunkReceived[chunkId]) {
            LOGGER.debug("Chunk {} already received for session {}", chunkId, session.sessionId);
            sendAck(socket, session.sessionId, AckType.CHUNK, chunkId, "Duplicate chunk", address, port);
            return;
        }
        Path chunkPath = session.sessionDir.resolve(String.format("chunk-%05d.part", chunkId));
        Files.write(chunkPath, packet.getPayload(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        session.chunkReceived[chunkId] = true;
        session.receivedChunks++;
        session.receivedBytes += packet.getPayload().length;
        sendAck(socket, session.sessionId, AckType.CHUNK, chunkId, "Chunk received", address, port);
        if (session.isComplete()) {
            LOGGER.info("All chunks received for session {}. Assembling...", session.sessionId);
            assembleAndValidate(session, socket, address, port);
        }
    }

    private void assembleAndValidate(TransferSession session, DatagramSocket socket, InetAddress address, int port) throws IOException {
        Path assembledFile = session.sessionDir.resolve(session.fileName + ".assembled");
        try (OutputStream outputStream = Files.newOutputStream(assembledFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (int i = 0; i < session.totalChunks; i++) {
                Path chunk = session.sessionDir.resolve(String.format("chunk-%05d.part", i));
                byte[] data = Files.readAllBytes(chunk);
                outputStream.write(data);
            }
        }
        String checksum = ChecksumUtil.sha256(assembledFile);
        if (!checksum.equalsIgnoreCase(session.expectedChecksum)) {
            LOGGER.warn("Checksum mismatch for session {}. Expected {}, got {}", session.sessionId, session.expectedChecksum, checksum);
            storageManager.cleanupSession(session.sessionId);
            sessions.remove(session.sessionId);
            sendAck(socket, session.sessionId, AckType.RETRY, -1, "Checksum mismatch", address, port);
            return;
        }
        SecurityResult securityResult = securityChecker.evaluate(assembledFile);
        Path targetPath;
        if (securityResult.isSafe()) {
            targetPath = storageManager.resolveIncomingPath(session.fileName);
        } else if (securityChecker.isQuarantineEnabled()) {
            targetPath = storageManager.resolveQuarantinePath(session.fileName);
        } else {
            targetPath = storageManager.resolveIncomingPath(session.fileName);
        }
        storageManager.move(assembledFile, targetPath);
        LOGGER.info("Session {} stored at {} ({})", session.sessionId, targetPath, securityResult.getMessage());
        storageManager.cleanupSession(session.sessionId);
        sessions.remove(session.sessionId);
        AckType ackType = securityResult.isSafe() ? AckType.COMPLETE : AckType.REJECTED;
        sendAck(socket, session.sessionId, ackType, -1, securityResult.getMessage(), address, port);
        if (securityResult.isSafe()) {
            triggerWebhook(session, targetPath, securityResult);
        }
    }

    private void triggerWebhook(TransferSession session, Path targetPath, SecurityResult result) {
        if (n8nClient == null) {
            return;
        }
        WebhookPayload payload = WebhookPayload.builder()
                .withFileName(session.fileName)
                .withFilePath(targetPath.toAbsolutePath().toString())
                .withFileSize(session.fileSize)
                .withChecksum(session.expectedChecksum)
                .withMimeType(Optional.ofNullable(result.getActualMime()).orElse("unknown"))
                .withReceivedFrom(session.senderPeerId)
                .withTimestamp(Instant.now().toEpochMilli())
                .withSecurityStatus("SAFE")
                .build();
        n8nClient.sendAsync(payload);
    }

    private void sendAck(DatagramSocket socket, UUID sessionId, AckType ackType, int chunkId, String message, InetAddress address, int port) {
        try {
            Packet ack = Packet.ack(sessionId, ackType, chunkId, message);
            socket.send(new java.net.DatagramPacket(ack.toBytes(), ack.toBytes().length, address, port));
        } catch (IOException e) {
            LOGGER.error("Failed to send ACK {} for session {}", ackType, sessionId, e);
        }
    }

    private static final class TransferSession {
        private final UUID sessionId;
        private final String fileName;
        private final String expectedChecksum;
        private final int totalChunks;
        private final long fileSize;
        private final String senderPeerId;
        private final Path sessionDir;
        private final boolean[] chunkReceived;
        private int receivedChunks;
        private long receivedBytes;

        private TransferSession(UUID sessionId, String fileName, String expectedChecksum, int totalChunks, long fileSize, String senderPeerId, Path sessionDir) {
            this.sessionId = sessionId;
            this.fileName = fileName;
            this.expectedChecksum = expectedChecksum;
            this.totalChunks = totalChunks;
            this.fileSize = fileSize;
            this.senderPeerId = senderPeerId;
            this.sessionDir = sessionDir;
            this.chunkReceived = new boolean[totalChunks];
        }

        private boolean isComplete() {
            return receivedChunks == totalChunks && Arrays.stream(chunkReceived).allMatch(received -> received);
        }
    }
}
