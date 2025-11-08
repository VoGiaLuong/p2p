package com.example.p2p.client;

import com.example.p2p.classification.FileTypeClassifier;
import com.example.p2p.n8n.N8nWebhookClient;
import com.example.p2p.transport.UdpFileSender;
import okhttp3.OkHttpClient;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DesktopClientApp {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: DesktopClientApp <filePath> <receiverHost>");
            return;
        }
        Path file = Path.of(args[0]);
        InetAddress receiver = InetAddress.getByName(args[1]);

        FileTypeClassifier classifier = new FileTypeClassifier(Map.of(
                "jpg", "media/images",
                "png", "media/images",
                "mp4", "media/videos",
                "pdf", "documents",
                "txt", "documents/text",
                "zip", "archives"
        ));

        UdpFileSender sender = new UdpFileSender(8 * 1024);
        FileTypeClassifier.ClassificationResult result = classifier.classify(file);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", file.getFileName().toString());
        metadata.put("size", Files.size(file));
        metadata.put("label", result.label());
        metadata.put("mimeType", result.mimeType());

        N8nWebhookClient webhookClient = new N8nWebhookClient(new OkHttpClient(),
                "http://localhost:5678/webhook/file-prepared");
        webhookClient.triggerWorkflow(file, metadata);

        sender.sendFile(file, receiver, 9_999);
    }
}
