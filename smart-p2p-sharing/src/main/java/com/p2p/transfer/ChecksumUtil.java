package com.p2p.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ChecksumUtil {

    private ChecksumUtil() {
    }

    public static String sha256(Path file) throws IOException {
        MessageDigest digest = createDigest("SHA-256");
        try (InputStream inputStream = Files.newInputStream(file); DigestInputStream dis = new DigestInputStream(inputStream, digest)) {
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {
                // reading stream updates digest automatically
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest createDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Digest algorithm not available: " + algorithm, e);
        }
    }
}
