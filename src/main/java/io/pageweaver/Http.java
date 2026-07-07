package io.pageweaver;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Thin {@link HttpClient} + Gson wrapper shared by every resource: attaches the API key, serializes
 * JSON, applies a timeout, and maps non-2xx responses to {@link PageWeaverException}. Package-private.
 */
final class Http {

    static final String DEFAULT_BASE_URL = "https://api.pageweaver.io";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();
    private static final Type LIST_TYPE = new TypeToken<List<Object>>() {}.getType();

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient client;
    private final Gson gson = new Gson();

    Http(String apiKey, String baseUrl) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("apiKey is required");
        }
        String base = baseUrl == null ? DEFAULT_BASE_URL : baseUrl;
        this.apiKey = apiKey;
        this.baseUrl = base.replaceAll("/+$", "");
        this.client = HttpClient.newHttpClient();
    }

    Gson gson() {
        return gson;
    }

    String baseUrl() {
        return baseUrl;
    }

    /** URL-encode a path segment (id). */
    static String enc(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8);
    }

    /** Perform a request and parse a JSON object response into a Map. */
    Map<String, Object> request(String method, String path, Map<String, Object> query,
                                Object body, Map<String, String> headers, boolean noAuth) {
        HttpResponse<String> resp = send(method, path, query, body, headers, noAuth);
        String raw = resp.body();
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> parsed = gson.fromJson(raw, MAP_TYPE);
        return parsed == null ? Map.of() : parsed;
    }

    /** Perform a request and parse a JSON array response into a List. */
    List<Object> requestList(String method, String path, Map<String, Object> query,
                             Object body, Map<String, String> headers, boolean noAuth) {
        HttpResponse<String> resp = send(method, path, query, body, headers, noAuth);
        String raw = resp.body();
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        List<Object> parsed = gson.fromJson(raw, LIST_TYPE);
        return parsed == null ? new ArrayList<>() : parsed;
    }

    /** Perform a request and return the raw response body as bytes (for downloads). */
    byte[] requestBytes(String method, String path, Map<String, Object> query,
                        Object body, Map<String, String> headers, boolean noAuth) {
        HttpRequest req = build(method, path, query, body, headers, noAuth,
                HttpRequest.BodyPublishers.noBody());
        try {
            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            int status = resp.statusCode();
            if (status < 200 || status >= 300) {
                String errBody = new String(resp.body(), StandardCharsets.UTF_8);
                throw apiError(method, path, status, errBody);
            }
            return resp.body();
        } catch (IOException e) {
            throw new PageWeaverException("HTTP request failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PageWeaverException("HTTP request interrupted: " + e.getMessage());
        }
    }

    /** Fetch an absolute URL (e.g. a signed download URL) with no auth header and return its bytes. */
    byte[] fetchUrlBytes(String url) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(DEFAULT_TIMEOUT)
                .build();
        try {
            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            int status = resp.statusCode();
            if (status < 200 || status >= 300) {
                throw new PageWeaverException("Failed to download from " + url + ": " + status, status,
                        new String(resp.body(), StandardCharsets.UTF_8));
            }
            return resp.body();
        } catch (IOException e) {
            throw new PageWeaverException("HTTP request failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PageWeaverException("HTTP request interrupted: " + e.getMessage());
        }
    }

    /**
     * Perform a request and return the raw String response (2xx only; non-2xx throws). For
     * content-negotiated endpoints (synchronous create) where the caller inspects headers + body.
     */
    HttpResponse<byte[]> requestRaw(String method, String path, Map<String, Object> query,
                                    Object body, Map<String, String> headers, boolean noAuth) {
        HttpRequest req = build(method, path, query, body, headers, noAuth,
                body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(gson.toJson(body)));
        try {
            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            int status = resp.statusCode();
            if (status < 200 || status >= 300) {
                String errBody = new String(resp.body(), StandardCharsets.UTF_8);
                throw apiError(method, path, status, errBody);
            }
            return resp;
        } catch (IOException e) {
            throw new PageWeaverException("HTTP request failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PageWeaverException("HTTP request interrupted: " + e.getMessage());
        }
    }

    private HttpResponse<String> send(String method, String path, Map<String, Object> query,
                                      Object body, Map<String, String> headers, boolean noAuth) {
        HttpRequest req = build(method, path, query, body, headers, noAuth,
                body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(gson.toJson(body)));
        try {
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            int status = resp.statusCode();
            if (status < 200 || status >= 300) {
                throw apiError(method, path, status, resp.body());
            }
            return resp;
        } catch (IOException e) {
            throw new PageWeaverException("HTTP request failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PageWeaverException("HTTP request interrupted: " + e.getMessage());
        }
    }

    private HttpRequest build(String method, String path, Map<String, Object> query, Object body,
                              Map<String, String> headers, boolean noAuth,
                              HttpRequest.BodyPublisher publisher) {
        String url = baseUrl + path + buildQuery(query);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(DEFAULT_TIMEOUT);

        if (!noAuth) {
            builder.header("x-api-key", apiKey);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        if (headers != null) {
            for (Map.Entry<String, String> h : headers.entrySet()) {
                if (h.getValue() != null) {
                    builder.header(h.getKey(), h.getValue());
                }
            }
        }
        builder.method(method, publisher);
        return builder.build();
    }

    /** Build a query string, omitting null/empty values. */
    private static String buildQuery(Map<String, Object> query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : query.entrySet()) {
            Object v = e.getValue();
            if (v == null) {
                continue;
            }
            String s = String.valueOf(v);
            if (s.isEmpty()) {
                continue;
            }
            sb.append(sb.length() == 0 ? '?' : '&');
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(s, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    /** Map a non-2xx response into a PageWeaverException, extracting body.message (String or array). */
    private PageWeaverException apiError(String method, String path, int status, String raw) {
        String message = null;
        try {
            if (raw != null && !raw.isEmpty()) {
                Map<String, Object> body = gson.fromJson(raw, MAP_TYPE);
                if (body != null) {
                    Object m = body.get("message");
                    if (m instanceof String) {
                        message = (String) m;
                    } else if (m instanceof List) {
                        List<?> parts = (List<?>) m;
                        StringBuilder sb = new StringBuilder();
                        for (Object p : parts) {
                            if (sb.length() > 0) {
                                sb.append(", ");
                            }
                            sb.append(String.valueOf(p));
                        }
                        message = sb.toString();
                    }
                }
            }
        } catch (JsonSyntaxException ignored) {
            // Non-JSON body: fall through to the default message.
        }
        if (message == null || message.isEmpty()) {
            message = method + " " + path + " failed with status " + status;
        }
        return new PageWeaverException(message, status, raw);
    }
}
