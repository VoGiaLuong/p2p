package com.p2p.network;

public enum PacketType {
    METADATA((byte) 0),
    DATA((byte) 1),
    ACK((byte) 2),
    DISCOVERY((byte) 3),
    DISCOVERY_RESPONSE((byte) 4);

    private final byte code;

    PacketType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static PacketType fromCode(byte code) {
        for (PacketType value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown packet type code: " + code);
    }
}
