package com.p2p.security;

import org.apache.tika.Tika;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class MimeDetector {

    private final Tika tika = new Tika();

    public String detect(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        return tika.detect(file);
    }
}
