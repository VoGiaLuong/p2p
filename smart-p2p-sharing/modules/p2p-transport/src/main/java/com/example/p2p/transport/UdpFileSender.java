package com.example.p2p.transport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Skeleton UDP sender that demonstrates how files could be chunked and pushed across peers.
 */
public class UdpFileSender {
    private final int chunkSize;

    public UdpFileSender(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public void sendFile(Path file, InetAddress address, int port) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] data = Files.readAllBytes(file);
            for (int offset = 0; offset < data.length; offset += chunkSize) {
                int length = Math.min(chunkSize, data.length - offset);
                DatagramPacket packet = new DatagramPacket(data, offset, length, address, port);
                socket.send(packet);
            }
        }
    }
}
