package com.p2p.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.Duration;
import java.util.Objects;

public class UDPClient implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger(UDPClient.class);

    private final DatagramSocket socket;

    public UDPClient() throws SocketException {
        this.socket = new DatagramSocket();
    }

    public UDPClient(int localPort) throws SocketException {
        this.socket = new DatagramSocket(localPort);
    }

    public synchronized void send(Packet packet, InetSocketAddress target) throws IOException {
        Objects.requireNonNull(packet, "packet");
        Objects.requireNonNull(target, "target");
        byte[] payload = packet.toBytes();
        DatagramPacket datagramPacket = new DatagramPacket(payload, payload.length, target);
        socket.send(datagramPacket);
        LOGGER.debug("Sent {} to {}", packet, target);
    }

    public synchronized Packet receive(Duration timeout) throws IOException {
        Objects.requireNonNull(timeout, "timeout");
        socket.setSoTimeout((int) timeout.toMillis());
        byte[] buffer = new byte[65535];
        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(datagramPacket);
        byte[] data = new byte[datagramPacket.getLength()];
        System.arraycopy(buffer, 0, data, 0, datagramPacket.getLength());
        Packet packet = Packet.fromBytes(data);
        LOGGER.debug("Received {} from {}:{}", packet, datagramPacket.getAddress(), datagramPacket.getPort());
        return packet;
    }

    @Override
    public void close() {
        socket.close();
    }

    public DatagramSocket getSocket() {
        return socket;
    }
}
