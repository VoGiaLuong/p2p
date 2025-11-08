package com.example.p2p.classification;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Demonstrates how AI-backed classification could be layered on top of MIME sniffing.
 * Real implementations could delegate to an inference server or embedded model.
 */
public class FileTypeClassifier {

    private final Map<String, String> extensionLabels;

    public FileTypeClassifier(Map<String, String> extensionLabels) {
        this.extensionLabels = extensionLabels;
    }

    public ClassificationResult classify(Path file) {
        String mime = "unknown";
        try {
            mime = Files.probeContentType(file);
        } catch (Exception ignored) {
        }
        String label = extensionLabels.getOrDefault(getExtension(file), "uncategorized");
        return new ClassificationResult(label, mime);
    }

    private String getExtension(Path file) {
        String name = file.getFileName().toString();
        int dotIndex = name.lastIndexOf('.');
        return dotIndex == -1 ? "" : name.substring(dotIndex + 1).toLowerCase();
    }

    public record ClassificationResult(String label, String mimeType) {
    }
}
