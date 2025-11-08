package com.p2p.security;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class SecurityChecker {

    public static final class SecurityResult {
        private final boolean safe;
        private final String expectedMime;
        private final String actualMime;
        private final String message;

        public SecurityResult(boolean safe, String expectedMime, String actualMime, String message) {
            this.safe = safe;
            this.expectedMime = expectedMime;
            this.actualMime = actualMime;
            this.message = message;
        }

        public boolean isSafe() {
            return safe;
        }

        public String getExpectedMime() {
            return expectedMime;
        }

        public String getActualMime() {
            return actualMime;
        }

        public String getMessage() {
            return message;
        }
    }

    private final MimeDetector mimeDetector;
    private final Map<String, String> allowedMimeMappings;
    private final boolean enableQuarantine;

    public SecurityChecker(MimeDetector mimeDetector, Map<String, String> allowedMimeMappings, boolean enableQuarantine) {
        this.mimeDetector = Objects.requireNonNull(mimeDetector, "mimeDetector");
        this.allowedMimeMappings = Objects.requireNonNull(allowedMimeMappings, "allowedMimeMappings");
        this.enableQuarantine = enableQuarantine;
    }

    public SecurityResult evaluate(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        String actualMime = mimeDetector.detect(file);
        String extension = extractExtension(file.getFileName().toString());
        String expectedMime = extension != null ? allowedMimeMappings.get(extension) : null;
        if (expectedMime == null) {
            return new SecurityResult(true, null, actualMime, "No MIME mapping configured for extension " + extension);
        }
        if (expectedMime.equalsIgnoreCase(actualMime)) {
            return new SecurityResult(true, expectedMime, actualMime, "MIME validation succeeded");
        }
        return new SecurityResult(false, expectedMime, actualMime, "Detected MIME does not match expected mapping");
    }

    public boolean isQuarantineEnabled() {
        return enableQuarantine;
    }

    private String extractExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index <= 0 || index == filename.length() - 1) {
            return null;
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
