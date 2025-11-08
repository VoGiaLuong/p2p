package com.p2p.webhook;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class N8nClient {

    private static final Logger LOGGER = LogManager.getLogger(N8nClient.class);

    private final HttpClient httpClient;
    private final URI webhookUri;
    private final String basicAuthHeader;
    private final Gson gson = new Gson();

    public N8nClient(URI webhookUri, Duration timeout, String basicAuthUser, String basicAuthPassword) {
        this.webhookUri = Objects.requireNonNull(webhookUri, "webhookUri");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        if (basicAuthUser != null && !basicAuthUser.isBlank()) {
            String token = basicAuthUser + ":" + (basicAuthPassword == null ? "" : basicAuthPassword);
            this.basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        } else {
            this.basicAuthHeader = null;
        }
    }

    public CompletableFuture<Void> sendAsync(WebhookPayload payload) {
        return CompletableFuture.runAsync(() -> {
            try {
                send(payload);
            } catch (Exception ex) {
                LOGGER.error("Failed to notify n8n webhook", ex);
            }
        });
    }

    public void send(WebhookPayload payload) throws Exception {
        Objects.requireNonNull(payload, "payload");
        String json = gson.toJson(payload);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(webhookUri)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (basicAuthHeader != null) {
            builder.header("Authorization", basicAuthHeader);
        }
        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            LOGGER.info("n8n webhook acknowledged payload for {}", payload.getFileName());
        } else {
            LOGGER.warn("n8n webhook responded with status {}: {}", response.statusCode(), response.body());
        }
    }
}
