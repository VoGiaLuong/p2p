package com.example.p2p.n8n;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Simple wrapper around the n8n webhook endpoint used to trigger automations with
 * metadata generated from the file classification stage.
 */
public class N8nWebhookClient {

    private static final MediaType JSON = MediaType.parse("application/json");

    private final OkHttpClient httpClient;
    private final String webhookUrl;

    public N8nWebhookClient(OkHttpClient httpClient, String webhookUrl) {
        this.httpClient = httpClient;
        this.webhookUrl = webhookUrl;
    }

    public void triggerWorkflow(Path file, Map<String, Object> metadata) throws IOException {
        String payload = buildJsonPayload(file, metadata);
        RequestBody body = RequestBody.create(payload, JSON);
        Request request = new Request.Builder()
                .url(webhookUrl)
                .post(body)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to trigger n8n webhook: " + response);
            }
        }
    }

    private String buildJsonPayload(Path file, Map<String, Object> metadata) {
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        joiner.add("\"filePath\":\"" + escape(file.toAbsolutePath().toString()) + "\"");
        metadata.forEach((key, value) -> {
            String val = value == null ? "null" : toJsonValue(value);
            joiner.add("\"" + escape(key) + "\":" + val);
        });
        return joiner.toString();
    }

    private String toJsonValue(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private String escape(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
