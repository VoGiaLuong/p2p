package com.p2p.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class UDPServer implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger(UDPServer.class);

    public interface PacketHandler {
        void handle(Packet packet, InetAddress address, int port, DatagramSocket socket);
    }

    private final DatagramSocket socket;
    private final PacketHandler handler;
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Future<?> listenerTask;

    public UDPServer(int port, PacketHandler handler) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.handler = Objects.requireNonNull(handler, "handler");
        this.executor = Executors.newCachedThreadPool();
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            listenerTask = executor.submit(this::listen);
            LOGGER.info("UDP server started on port {}", socket.getLocalPort());
        }
    }

    private void listen() {
        byte[] buffer = new byte[65535];
        while (running.get()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                Packet decoded = Packet.fromBytes(data);
                executor.execute(() -> handler.handle(decoded, packet.getAddress(), packet.getPort(), socket));
            } catch (IOException e) {
                if (running.get()) {
                    LOGGER.error("Error while receiving UDP packet", e);
                } else {
                    LOGGER.debug("UDP server socket closed");
                }
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        if (listenerTask != null) {
            listenerTask.cancel(true);
        }
        socket.close();
        executor.shutdownNow();
        LOGGER.info("UDP server on port {} stopped", socket.getLocalPort());
    }

    public int getPort() {
        return socket.getLocalPort();
    }
}
