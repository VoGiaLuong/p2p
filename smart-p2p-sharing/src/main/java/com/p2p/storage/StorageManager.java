package com.p2p.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

public class StorageManager {

    private static final Logger LOGGER = LogManager.getLogger(StorageManager.class);

    private final Path baseDir;
    private final Path incomingDir;
    private final Path organizedDir;
    private final Path quarantineDir;
    private final Path tempDir;

    public StorageManager(Path baseDir, Path incomingDir, Path organizedDir, Path quarantineDir, Path tempDir) {
        this.baseDir = Objects.requireNonNull(baseDir, "baseDir");
        this.incomingDir = Objects.requireNonNull(incomingDir, "incomingDir");
        this.organizedDir = Objects.requireNonNull(organizedDir, "organizedDir");
        this.quarantineDir = Objects.requireNonNull(quarantineDir, "quarantineDir");
        this.tempDir = Objects.requireNonNull(tempDir, "tempDir");
    }

    public void initialize() throws IOException {
        Files.createDirectories(baseDir);
        Files.createDirectories(incomingDir);
        Files.createDirectories(organizedDir);
        Files.createDirectories(quarantineDir);
        Files.createDirectories(tempDir);
        LOGGER.info("Storage directories initialised under {}", baseDir.toAbsolutePath());
    }

    public Path getIncomingDir() {
        return incomingDir;
    }

    public Path getOrganizedDir() {
        return organizedDir;
    }

    public Path getQuarantineDir() {
        return quarantineDir;
    }

    public Path getTempDir() {
        return tempDir;
    }

    public Path createSessionTempDirectory(UUID sessionId) throws IOException {
        Path sessionDir = tempDir.resolve(sessionId.toString());
        Files.createDirectories(sessionDir);
        return sessionDir;
    }

    public Path resolveIncomingPath(String fileName) throws IOException {
        return resolveUniquePath(incomingDir, fileName);
    }

    public Path resolveQuarantinePath(String fileName) throws IOException {
        return resolveUniquePath(quarantineDir, fileName);
    }

    public void move(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("Moved {} to {}", source, target);
    }

    public void cleanupSession(UUID sessionId) {
        Path sessionDir = tempDir.resolve(sessionId.toString());
        try {
            if (Files.exists(sessionDir)) {
                Files.walk(sessionDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                LOGGER.warn("Failed to delete {}", path, e);
                            }
                        });
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to clean up session directory {}", sessionDir, e);
        }
    }

    private Path resolveUniquePath(Path directory, String fileName) throws IOException {
        Files.createDirectories(directory);
        Path candidate = directory.resolve(fileName);
        if (!Files.exists(candidate)) {
            return candidate;
        }
        String name = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            name = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }
        int counter = 1;
        while (Files.exists(candidate)) {
            candidate = directory.resolve(name + "(" + counter++ + ")" + extension);
        }
        return candidate;
    }
}
