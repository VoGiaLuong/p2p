package com.p2p.network;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Serialises and deserialises UDP packets exchanged between peers.
 *
 * <pre>
 * Header layout (32 bytes):
 * [0]        - packet type
 * [1..16]    - session UUID (MSB + LSB)
 * [17..20]   - chunk id (int)
 * [21..24]   - total chunks (int)
 * [25]       - acknowledgement type (if ACK)
 * [26..29]   - reserved / future use (currently payload length high bits)
 * [30..31]   - payload length low bits
 * After header - payload bytes (metadata JSON, chunk data, or message).
 * </pre>
 */
public final class Packet {

    private static final int HEADER_SIZE = 32;

    private final PacketType packetType;
    private final UUID sessionId;
    private final int chunkId;
    private final int totalChunks;
    private final AckType ackType;
    private final byte[] payload;

    private Packet(PacketType packetType,
                   UUID sessionId,
                   int chunkId,
                   int totalChunks,
                   AckType ackType,
                   byte[] payload) {
        this.packetType = Objects.requireNonNull(packetType, "packetType");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.chunkId = chunkId;
        this.totalChunks = totalChunks;
        this.ackType = ackType;
        this.payload = payload == null ? new byte[0] : payload.clone();
    }

    public static Packet metadata(UUID sessionId, byte[] metadataBytes) {
        return new Packet(PacketType.METADATA, sessionId, -1, -1, null, metadataBytes);
    }

    public static Packet data(UUID sessionId, int chunkId, int totalChunks, byte[] chunkBytes) {
        return new Packet(PacketType.DATA, sessionId, chunkId, totalChunks, null, chunkBytes);
    }

    public static Packet ack(UUID sessionId, AckType ackType, int chunkId, String message) {
        byte[] messageBytes = message == null ? new byte[0] : message.getBytes(StandardCharsets.UTF_8);
        return new Packet(PacketType.ACK, sessionId, chunkId, -1, Objects.requireNonNull(ackType, "ackType"), messageBytes);
    }

    public static Packet discovery(UUID sessionId, byte[] payload) {
        return new Packet(PacketType.DISCOVERY, sessionId, -1, -1, null, payload);
    }

    public static Packet discoveryResponse(UUID sessionId, byte[] payload) {
        return new Packet(PacketType.DISCOVERY_RESPONSE, sessionId, -1, -1, null, payload);
    }

    public PacketType getPacketType() {
        return packetType;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public int getChunkId() {
        return chunkId;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public Optional<AckType> getAckType() {
        return Optional.ofNullable(ackType);
    }

    public byte[] getPayload() {
        return payload.clone();
    }

    public byte[] toBytes() {
        int payloadLength = payload.length;
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + payloadLength);
        buffer.put(packetType.getCode());
        buffer.putLong(sessionId.getMostSignificantBits());
        buffer.putLong(sessionId.getLeastSignificantBits());
        buffer.putInt(chunkId);
        buffer.putInt(totalChunks);
        buffer.put(ackType != null ? ackType.getCode() : (byte) -1);
        buffer.putInt(payloadLength);
        buffer.putShort((short) 0); // reserved for future use
        buffer.put(payload);
        return buffer.array();
    }

    public static Packet fromBytes(byte[] bytes) {
        if (bytes.length < HEADER_SIZE) {
            throw new IllegalArgumentException("Packet is too small: " + bytes.length);
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        PacketType packetType = PacketType.fromCode(buffer.get());
        long msb = buffer.getLong();
        long lsb = buffer.getLong();
        UUID sessionId = new UUID(msb, lsb);
        int chunkId = buffer.getInt();
        int totalChunks = buffer.getInt();
        byte ackCode = buffer.get();
        int payloadLength = buffer.getInt();
        buffer.getShort(); // reserved
        if (payloadLength < 0 || payloadLength > bytes.length - HEADER_SIZE) {
            throw new IllegalArgumentException("Invalid payload length: " + payloadLength);
        }
        byte[] payload = new byte[payloadLength];
        buffer.get(payload);
        AckType ackType = ackCode >= 0 ? AckType.fromCode(ackCode) : null;
        return new Packet(packetType, sessionId, chunkId, totalChunks, ackType, payload);
    }

    @Override
    public String toString() {
        return "Packet{" +
                "type=" + packetType +
                ", sessionId=" + sessionId +
                ", chunkId=" + chunkId +
                ", totalChunks=" + totalChunks +
                ", ackType=" + ackType +
                ", payloadLength=" + payload.length +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Packet packet)) return false;
        return chunkId == packet.chunkId &&
                totalChunks == packet.totalChunks &&
                packetType == packet.packetType &&
                Objects.equals(sessionId, packet.sessionId) &&
                ackType == packet.ackType &&
                Arrays.equals(payload, packet.payload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(packetType, sessionId, chunkId, totalChunks, ackType);
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }
}
