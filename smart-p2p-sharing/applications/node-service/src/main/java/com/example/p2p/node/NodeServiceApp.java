package com.example.p2p.node;

import com.example.p2p.classification.FileOrganizer;
import com.example.p2p.classification.FileTypeClassifier;
import com.example.p2p.n8n.N8nWebhookClient;
import com.example.p2p.transport.UdpFileReceiver;
import okhttp3.OkHttpClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class NodeServiceApp {

    public static void main(String[] args) throws Exception {
        Path storageRoot = Path.of(args.length > 0 ? args[0] : "../runtime/storage");
        FileOrganizer organizer = new FileOrganizer(storageRoot);
        organizer.initializeLayout();

        FileTypeClassifier classifier = new FileTypeClassifier(Map.of(
                "jpg", "media/images",
                "png", "media/images",
                "mp4", "media/videos",
                "pdf", "documents",
                "txt", "documents/text",
                "zip", "archives"
        ));

        UdpFileReceiver receiver = new UdpFileReceiver(9_999, 8 * 1024);
        Path incomingPath = organizer.allocateIncomingPath("incoming.bin");
        Path received = receiver.receiveFile(incomingPath);
        Path processingFile = organizer.markForProcessing(received);
        Path classified = organizer.classifyAndStore(processingFile, classifier);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", classified.getFileName().toString());
        metadata.put("relativePath", storageRoot.relativize(classified).toString());
        metadata.put("mimeType", Files.probeContentType(classified));

        N8nWebhookClient webhookClient = new N8nWebhookClient(new OkHttpClient(),
                "http://localhost:5678/webhook/file-received");
        webhookClient.triggerWorkflow(classified, metadata);
    }
}
