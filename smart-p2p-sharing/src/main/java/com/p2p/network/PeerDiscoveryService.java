package com.p2p.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PeerDiscoveryService implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger(PeerDiscoveryService.class);

    private final String peerId;
    private final int discoveryPort;
    private final int serverPort;
    private final DatagramSocket socket;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, PeerInfo> peers = new ConcurrentHashMap<>();
    private final Duration heartbeatInterval;

    public PeerDiscoveryService(String peerId, int discoveryPort, int serverPort, Duration heartbeatInterval) throws SocketException {
        this.peerId = Objects.requireNonNull(peerId, "peerId");
        this.discoveryPort = discoveryPort;
        this.serverPort = serverPort;
        this.heartbeatInterval = heartbeatInterval;
        this.socket = new DatagramSocket(null);
        this.socket.setReuseAddress(true);
        this.socket.bind(new InetSocketAddress(discoveryPort));
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::broadcastHeartbeat, 0, heartbeatInterval.toMillis(), TimeUnit.MILLISECONDS);
        scheduler.scheduleWithFixedDelay(this::receiveLoop, 0, 200, TimeUnit.MILLISECONDS);
        LOGGER.info("Peer discovery service started on port {}", discoveryPort);
    }

    private void broadcastHeartbeat() {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("type", "P2P_PEER_DISCOVERY");
            payload.addProperty("peerId", peerId);
            payload.addProperty("port", serverPort);
            Packet packet = Packet.discovery(UUID.randomUUID(), payload.toString().getBytes(StandardCharsets.UTF_8));
            byte[] bytes = packet.toBytes();
            DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, InetAddress.getByName("255.255.255.255"), discoveryPort);
            socket.setBroadcast(true);
            socket.send(datagramPacket);
            LOGGER.debug("Sent discovery heartbeat as {}", peerId);
        } catch (IOException ex) {
            LOGGER.error("Failed to broadcast heartbeat", ex);
        }
    }

    private void receiveLoop() {
        try {
            if (socket.getSoTimeout() != 200) {
                socket.setSoTimeout(200);
            }
            byte[] buffer = new byte[65535];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
            } catch (SocketTimeoutException ex) {
                return;
            }
            byte[] data = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
            Packet decoded = Packet.fromBytes(data);
            handleDiscoveryPacket(decoded, packet.getAddress());
        } catch (IOException ex) {
            LOGGER.error("Error while receiving discovery packet", ex);
        }
    }

    private void handleDiscoveryPacket(Packet packet, InetAddress sender) throws IOException {
        if (packet.getPacketType() == PacketType.DISCOVERY) {
            JsonObject payload = JsonParser.parseString(new String(packet.getPayload(), StandardCharsets.UTF_8)).getAsJsonObject();
            String senderPeerId = payload.get("peerId").getAsString();
            if (peerId.equals(senderPeerId)) {
                return; // ignore own heartbeats
            }
            int port = payload.get("port").getAsInt();
            peers.compute(senderPeerId, (id, info) -> {
                if (info == null) {
                    LOGGER.info("Discovered peer {} at {}:{}", id, sender.getHostAddress(), port);
                    return new PeerInfo(id, sender, port);
                }
                info.refresh();
                return info;
            });
            sendDiscoveryResponse(sender, port);
        } else if (packet.getPacketType() == PacketType.DISCOVERY_RESPONSE) {
            JsonObject payload = JsonParser.parseString(new String(packet.getPayload(), StandardCharsets.UTF_8)).getAsJsonObject();
            String senderPeerId = payload.get("peerId").getAsString();
            if (peerId.equals(senderPeerId)) {
                return;
            }
            int port = payload.get("port").getAsInt();
            peers.compute(senderPeerId, (id, info) -> {
                if (info == null) {
                    LOGGER.info("Received discovery response from {} at {}:{}", id, sender.getHostAddress(), port);
                    return new PeerInfo(id, sender, port);
                }
                info.refresh();
                return info;
            });
        }
    }

    private void sendDiscoveryResponse(InetAddress address, int port) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "P2P_PEER_RESPONSE");
        payload.addProperty("peerId", peerId);
        payload.addProperty("port", serverPort);
        Packet response = Packet.discoveryResponse(UUID.randomUUID(), payload.toString().getBytes(StandardCharsets.UTF_8));
        byte[] bytes = response.toBytes();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, discoveryPort);
        socket.send(packet);
    }

    public Collection<PeerInfo> getPeers() {
        return Collections.unmodifiableCollection(peers.values());
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        socket.close();
    }
}
