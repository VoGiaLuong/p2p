package com.p2p.network;

public enum AckType {
    METADATA((byte) 0),
    CHUNK((byte) 1),
    COMPLETE((byte) 2),
    RETRY((byte) 3),
    REJECTED((byte) 4);

    private final byte code;

    AckType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static AckType fromCode(byte code) {
        for (AckType value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown ack type code: " + code);
    }
}
