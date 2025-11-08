package com.example.p2p.classification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Utility responsible for placing incoming files into a predictable directory
 * structure and delegating to the {@link FileTypeClassifier} to identify the
 * proper destination folder inside the classified area.
 */
public class FileOrganizer {

    private final Path storageRoot;
    private final Path inboxDir;
    private final Path processingDir;
    private final Path classifiedDir;

    public FileOrganizer(Path storageRoot) {
        this.storageRoot = Objects.requireNonNull(storageRoot, "storageRoot");
        this.inboxDir = storageRoot.resolve("inbox");
        this.processingDir = storageRoot.resolve("processing");
        this.classifiedDir = storageRoot.resolve("classified");
    }

    public void initializeLayout() throws IOException {
        Files.createDirectories(storageRoot);
        Files.createDirectories(inboxDir);
        Files.createDirectories(processingDir);
        Files.createDirectories(classifiedDir);
    }

    public Path allocateIncomingPath(String originalFileName) throws IOException {
        initializeLayout();
        String sanitized = originalFileName == null || originalFileName.isBlank()
                ? "incoming-" + System.currentTimeMillis()
                : Path.of(originalFileName).getFileName().toString();
        Path target = inboxDir.resolve(sanitized);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return target;
    }

    public Path markForProcessing(Path file) throws IOException {
        initializeLayout();
        Path target = processingDir.resolve(file.getFileName());
        Files.createDirectories(target.getParent());
        return Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public Path classifyAndStore(Path file, FileTypeClassifier classifier) throws IOException {
        initializeLayout();
        FileTypeClassifier.ClassificationResult result = classifier.classify(file);
        Path datePartition = classifiedDir.resolve(result.label()).resolve(LocalDate.now().toString());
        Files.createDirectories(datePartition);
        Path destination = datePartition.resolve(file.getFileName().toString());
        return Files.move(file, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    public Path getInboxDir() {
        return inboxDir;
    }

    public Path getProcessingDir() {
        return processingDir;
    }

    public Path getClassifiedDir() {
        return classifiedDir;
    }
}
