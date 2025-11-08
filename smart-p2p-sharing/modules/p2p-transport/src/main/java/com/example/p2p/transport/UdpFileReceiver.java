package com.example.p2p.transport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal UDP receiver that buffers packets and flushes them to disk.
 */
public class UdpFileReceiver {

    private final int port;
    private final int chunkSize;

    public UdpFileReceiver(int port, int chunkSize) {
        this.port = port;
        this.chunkSize = chunkSize;
    }

    public Path receiveFile(Path destination) throws IOException {
        List<byte[]> buffers = new ArrayList<>();
        try (DatagramSocket socket = new DatagramSocket(port)) {
            socket.setSoTimeout(1_000);
            boolean receiving = true;
            while (receiving) {
                byte[] buffer = new byte[chunkSize];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                    byte[] actual = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, actual, 0, packet.getLength());
                    buffers.add(actual);
                    if (packet.getLength() < chunkSize) {
                        receiving = false;
                    }
                } catch (SocketTimeoutException e) {
                    receiving = false;
                }
            }
        }

        int totalBytes = buffers.stream().mapToInt(arr -> arr.length).sum();
        byte[] merged = new byte[totalBytes];
        int offset = 0;
        for (byte[] chunk : buffers) {
            System.arraycopy(chunk, 0, merged, offset, chunk.length);
            offset += chunk.length;
        }
        Files.createDirectories(destination.getParent());
        Files.write(destination, merged);
        return destination;
    }
}
