package io.pageweaver;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Client for the PageWeaver document-generation API. */
public final class PageWeaver {

    private static final String DEFAULT_BASE_URL = "https://api.pageweaver.io";
    private static final List<String> TERMINAL = Arrays.asList("done", "failed", "error");
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient http;
    private final Gson gson = new Gson();

    public PageWeaver(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL);
    }

    public PageWeaver(String apiKey, String baseUrl) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("apiKey is required");
        }
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newHttpClient();
    }

    /** POST /v1/documents. Pass the request body per the API docs. */
    public Map<String, Object> createDocument(Map<String, Object> body) {
        return request("POST", "/v1/documents", body);
    }

    /** GET /v1/documents/:id. */
    public Map<String, Object> getDocument(String id) {
        return request("GET", "/v1/documents/" + id, null);
    }

    /** Create a document and poll until it reaches a terminal state. */
    public Map<String, Object> createAndWait(Map<String, Object> body, Duration pollInterval, Duration timeout) {
        Map<String, Object> created = createDocument(body);
        Object id = created.get("id");
        if (!(id instanceof String) || ((String) id).isEmpty()) {
            return created;
        }
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            Map<String, Object> doc = getDocument((String) id);
            Object status = doc.get("status");
            if (status instanceof String && TERMINAL.contains(status)) {
                return doc;
            }
            if (System.nanoTime() >= deadline) {
                throw new PageWeaverException("Timed out waiting for document " + id);
            }
            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PageWeaverException("Interrupted while waiting for document " + id);
            }
        }
    }

    private Map<String, Object> request(String method, String path, Map<String, Object> body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("x-api-key", apiKey)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30));

        if (body != null) {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(gson.toJson(body)));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        try {
            HttpResponse<String> resp = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String raw = resp.body();
            int status = resp.statusCode();
            if (status < 200 || status >= 300) {
                throw new PageWeaverException(method + " " + path + " failed with status " + status, status, raw);
            }
            if (raw == null || raw.isEmpty()) {
                return Map.of();
            }
            return gson.fromJson(raw, MAP_TYPE);
        } catch (IOException e) {
            throw new PageWeaverException("HTTP request failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PageWeaverException("HTTP request interrupted: " + e.getMessage());
        }
    }
}
