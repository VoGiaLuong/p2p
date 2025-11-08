package com.example.p2p.client;

import com.example.p2p.classification.FileTypeClassifier;
import com.example.p2p.n8n.N8nWebhookClient;
import com.example.p2p.transport.UdpFileSender;
import okhttp3.OkHttpClient;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Map;

public class DesktopClientApp {

    public static void main(String[] args) throws Exception {
        UdpFileSender sender = new UdpFileSender(8 * 1024);
        FileTypeClassifier classifier = new FileTypeClassifier(Map.of(
                "jpg", "image",
                "png", "image",
                "mp4", "video",
                "pdf", "document"
        ));
        N8nWebhookClient webhookClient = new N8nWebhookClient(new OkHttpClient(),
                "http://localhost:5678/webhook/file-received");

        Path file = Path.of(args.length > 0 ? args[0] : "sample.txt");
        FileTypeClassifier.ClassificationResult result = classifier.classify(file);
        webhookClient.triggerWorkflow(file, Map.of(
                "fileName", file.getFileName().toString(),
                "label", result.label(),
                "mimeType", result.mimeType()
        ));

        sender.sendFile(file, InetAddress.getLocalHost(), 9_999);
    }
}
