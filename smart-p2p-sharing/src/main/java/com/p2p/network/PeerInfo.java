package com.p2p.network;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Objects;

public class PeerInfo {
    private final String peerId;
    private final InetAddress address;
    private final int port;
    private volatile Instant lastSeen;

    public PeerInfo(String peerId, InetAddress address, int port) {
        this.peerId = Objects.requireNonNull(peerId, "peerId");
        this.address = Objects.requireNonNull(address, "address");
        this.port = port;
        this.lastSeen = Instant.now();
    }

    public String getPeerId() {
        return peerId;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void refresh() {
        this.lastSeen = Instant.now();
    }
}
