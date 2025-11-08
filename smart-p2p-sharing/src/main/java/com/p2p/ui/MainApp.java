package com.p2p.ui;

import com.p2p.network.PeerDiscoveryService;
import com.p2p.network.UDPServer;
import com.p2p.security.MimeDetector;
import com.p2p.security.SecurityChecker;
import com.p2p.storage.StorageManager;
import com.p2p.transfer.FileReceiver;
import com.p2p.transfer.FileSender;
import com.p2p.webhook.N8nClient;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class MainApp extends Application {

    private static final Logger LOGGER = LogManager.getLogger(MainApp.class);

    private StorageManager storageManager;
    private UDPServer udpServer;
    private PeerDiscoveryService peerDiscoveryService;
    private FileSender fileSender;
    private FileReceiver fileReceiver;
    private N8nClient n8nClient;
    private String peerId;
    private MainController mainController;

    @Override
    public void init() throws Exception {
        Properties properties = loadProperties();
        peerId = resolvePeerId(properties);
        storageManager = buildStorageManager(properties);
        storageManager.initialize();

        SecurityChecker securityChecker = buildSecurityChecker(properties);
        n8nClient = buildN8nClient(properties);

        fileReceiver = new FileReceiver(storageManager, securityChecker, n8nClient);
        int serverPort = Integer.parseInt(properties.getProperty("udp.server.port", "9876"));
        udpServer = new UDPServer(serverPort, fileReceiver);
        udpServer.start();

        int chunkSize = Integer.parseInt(properties.getProperty("udp.chunk.size", "8192"));
        Duration ackTimeout = Duration.ofMillis(Long.parseLong(properties.getProperty("udp.client.timeoutMillis", "5000")));
        int maxRetries = Integer.parseInt(properties.getProperty("udp.max.retries", "5"));
        fileSender = new FileSender(peerId, chunkSize, ackTimeout, maxRetries);

        int discoveryPort = Integer.parseInt(properties.getProperty("udp.discovery.port", "9875"));
        peerDiscoveryService = new PeerDiscoveryService(peerId, discoveryPort, serverPort, Duration.ofSeconds(3));
        peerDiscoveryService.start();
    }

    @Override
    public void start(Stage primaryStage) {
        TransferController transferController = new TransferController();
        mainController = new MainController(peerId, peerDiscoveryService, fileSender, transferController);
        Scene scene = new Scene(mainController.build(primaryStage), 900, 600);
        primaryStage.setTitle("Smart P2P File Sharing");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (mainController != null) {
            mainController.stop();
        }
        try {
            if (peerDiscoveryService != null) {
                peerDiscoveryService.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to close peer discovery service", e);
        }
        if (udpServer != null) {
            udpServer.close();
        }
    }

    private Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = MainApp.class.getResourceAsStream("/application.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        }
        return properties;
    }

    private String resolvePeerId(Properties properties) throws IOException {
        String configured = properties.getProperty("peer.id");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        String hostname = InetAddress.getLocalHost().getHostName();
        return hostname + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private StorageManager buildStorageManager(Properties properties) {
        Path baseDir = normalizePath(properties.getProperty("storage.baseDir", "./shared-storage"));
        Path incomingDir = normalizePath(properties.getProperty("storage.incomingDir", baseDir.resolve("incoming").toString()), baseDir);
        Path organizedDir = normalizePath(properties.getProperty("storage.organizedDir", baseDir.resolve("organized").toString()), baseDir);
        Path quarantineDir = normalizePath(properties.getProperty("storage.quarantineDir", baseDir.resolve("quarantine").toString()), baseDir);
        Path tempDir = normalizePath(properties.getProperty("storage.tempDir", baseDir.resolve("temp").toString()), baseDir);
        return new StorageManager(baseDir, incomingDir, organizedDir, quarantineDir, tempDir);
    }

    private Path normalizePath(String value) {
        return Paths.get(value).toAbsolutePath().normalize();
    }

    private Path normalizePath(String value, Path baseDir) {
        String resolved = value.replace("${storage.baseDir}", baseDir.toString());
        return normalizePath(resolved);
    }

    private SecurityChecker buildSecurityChecker(Properties properties) {
        String mappings = properties.getProperty("security.allowedMimeMappings", "");
        Map<String, String> mimeMappings = parseMimeMappings(mappings);
        boolean enableQuarantine = Boolean.parseBoolean(properties.getProperty("security.enableQuarantine", "true"));
        return new SecurityChecker(new MimeDetector(), mimeMappings, enableQuarantine);
    }

    private Map<String, String> parseMimeMappings(String mappings) {
        Map<String, String> result = new HashMap<>();
        if (mappings == null || mappings.isBlank()) {
            return result;
        }
        String[] pairs = mappings.split(",");
        for (String pair : pairs) {
            String[] parts = pair.split(":");
            if (parts.length == 2) {
                result.put(parts[0].trim().toLowerCase(), parts[1].trim());
            }
        }
        return result;
    }

    private N8nClient buildN8nClient(Properties properties) {
        String url = properties.getProperty("n8n.webhookUrl", "");
        if (url == null || url.isBlank()) {
            return null;
        }
        boolean authActive = Boolean.parseBoolean(properties.getProperty("n8n.auth.active", "false"));
        String username = authActive ? properties.getProperty("n8n.auth.basic.user", "") : null;
        String password = authActive ? properties.getProperty("n8n.auth.basic.password", "") : null;
        return new N8nClient(URI.create(url), Duration.ofSeconds(10), username, password);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
