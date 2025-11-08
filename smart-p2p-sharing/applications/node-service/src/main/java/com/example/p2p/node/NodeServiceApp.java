package com.example.p2p.node;

import com.example.p2p.classification.FileTypeClassifier;
import com.example.p2p.transport.UdpFileReceiver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class NodeServiceApp {

    public static void main(String[] args) throws Exception {
        Path storageRoot = Path.of(args.length > 0 ? args[0] : "./storage");
        Files.createDirectories(storageRoot);

        FileTypeClassifier classifier = new FileTypeClassifier(Map.of(
                "jpg", "media/images",
                "png", "media/images",
                "mp4", "media/videos",
                "pdf", "docs"
        ));

        UdpFileReceiver receiver = new UdpFileReceiver(9_999, 8 * 1024);
        Path incomingFile = storageRoot.resolve("incoming.bin");
        Path file = receiver.receiveFile(incomingFile);
        FileTypeClassifier.ClassificationResult result = classifier.classify(file);

        Path targetDir = storageRoot.resolve(result.label());
        Files.createDirectories(targetDir);
        Files.move(file, targetDir.resolve(file.getFileName().toString()));
    }
}
