package io.pageweaver;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Operations on documents: the core of the API. */
public final class Documents {

    private static final List<String> TERMINAL = Arrays.asList("done", "failed");

    private final Http http;

    Documents(Http http) {
        this.http = http;
    }

    /** Create a document from a template (validated payload) or inline HTML. Returns {@code 202}. */
    public Map<String, Object> create(Map<String, Object> body) {
        return http.request("POST", "/v1/documents", null, body, null, false);
    }

    /** Create a document with an idempotency key (deduplicates retries). */
    public Map<String, Object> create(Map<String, Object> body, String idempotencyKey) {
        Map<String, String> headers = new HashMap<>();
        headers.put("idempotency-key", idempotencyKey);
        return http.request("POST", "/v1/documents", null, body, headers, false);
    }

    /** Fetch the current state of a document. When {@code status} is "done" it carries a {@code download} block. */
    public Map<String, Object> get(String id) {
        return http.request("GET", "/v1/documents/" + Http.enc(id), null, null, null, false);
    }

    /** Fetch a document's integrity proof (content hash + hash-chain position). */
    public Map<String, Object> verify(String id) {
        return http.request("GET", "/v1/documents/" + Http.enc(id) + "/verify", null, null, null, false);
    }

    /** One page of the document history, newest first. Use {@code nextCursor} to page. */
    public Map<String, Object> list(Map<String, Object> params) {
        Map<String, Object> query = new HashMap<>();
        if (params != null) {
            query.put("status", params.get("status"));
            query.put("templateId", params.get("templateId"));
            query.put("cursor", params.get("cursor"));
            query.put("limit", params.get("limit"));
        }
        return http.request("GET", "/v1/documents", query, null, null, false);
    }

    /** Iterate every document across all pages, following {@code nextCursor}. */
    @SuppressWarnings("unchecked")
    public List<Object> listAll(Map<String, Object> params) {
        List<Object> all = new ArrayList<>();
        Map<String, Object> q = new HashMap<>();
        if (params != null) {
            q.putAll(params);
        }
        Object cursor = null;
        do {
            q.put("cursor", cursor);
            Map<String, Object> page = list(q);
            Object items = page.get("items");
            if (items instanceof List) {
                all.addAll((List<Object>) items);
            }
            cursor = page.get("nextCursor");
        } while (cursor != null && !String.valueOf(cursor).isEmpty());
        return all;
    }

    /** Faithfully replay a prior document (same version/payload/options). Returns a new id ({@code 202}). */
    public Map<String, Object> regenerate(String id) {
        return http.request("POST", "/v1/documents/" + Http.enc(id) + "/regenerate", null, null, null, false);
    }

    /**
     * Poll a document until it reaches a terminal state (or the timeout elapses). Throws
     * {@link PageWeaverTimeoutException} on timeout and {@link PageWeaverDocumentFailedException} on
     * failure (unless {@code throwOnFailure} is false).
     */
    public Map<String, Object> waitFor(String id, WaitOptions opts) {
        WaitOptions o = opts == null ? new WaitOptions() : opts;
        long deadline = System.currentTimeMillis() + o.timeoutMs;
        double delay = o.intervalMs;

        Map<String, Object> last = get(id);
        while (!isTerminal(last)) {
            if (System.currentTimeMillis() >= deadline) {
                throw new PageWeaverTimeoutException(id, statusOf(last), o.timeoutMs);
            }
            long remaining = deadline - System.currentTimeMillis();
            long sleep = Math.min((long) delay, Math.max(0, remaining));
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PageWeaverException("Interrupted while waiting for document " + id);
            }
            delay = Math.min(delay * o.backoff, o.maxIntervalMs);
            last = get(id);
        }
        if ("failed".equals(statusOf(last)) && o.throwOnFailure) {
            throw new PageWeaverDocumentFailedException(last);
        }
        return last;
    }

    /** Convenience: {@link #create} then {@link #waitFor}. */
    public Map<String, Object> createAndWait(Map<String, Object> body, WaitOptions opts) {
        Map<String, Object> created = create(body);
        Object id = created.get("id");
        return waitFor(String.valueOf(id), opts);
    }

    /**
     * Create a document synchronously: send {@code Prefer: wait} so the server holds the response open
     * until the render finishes. Returns a {@link SyncResult}. Pass {@code pdf=true} to stream raw PDF
     * bytes for an unprotected document; otherwise receive the finished document as JSON.
     */
    public SyncResult createSync(Map<String, Object> body, boolean pdf) {
        Map<String, String> headers = new HashMap<>();
        headers.put("prefer", "wait");
        if (pdf) {
            headers.put("accept", "application/pdf");
        }
        HttpResponse<byte[]> resp = http.requestRaw("POST", "/v1/documents", null, body, headers, false);
        String contentType = resp.headers().firstValue("content-type").orElse("");
        if (contentType.toLowerCase().contains("application/pdf")) {
            String id = resp.headers().firstValue("x-document-id").orElse(null);
            Integer version = parseIntOrNull(resp.headers().firstValue("x-document-version"));
            return SyncResult.pdf(id, version, resp.body());
        }
        String text = new String(resp.body(), java.nio.charset.StandardCharsets.UTF_8);
        Map<String, Object> parsed = text.isEmpty()
                ? Map.of()
                : http.gson().fromJson(text, new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType());
        if (parsed == null) {
            parsed = Map.of();
        }
        if (resp.statusCode() == 202) {
            Object id = parsed.get("id");
            Integer version = toInteger(parsed.get("version"));
            return SyncResult.pending(id == null ? null : String.valueOf(id), version,
                    (String) parsed.get("status"));
        }
        return SyncResult.document(parsed);
    }

    /** A document's per-page geometry plus whether extracted text and a thumbnail exist. */
    public List<Object> pages(String id) {
        return http.requestList("GET", "/v1/documents/" + Http.enc(id) + "/pages", null, null, null, false);
    }

    /** Carry open comment threads forward from a previous same-template document onto this one. Returns {@code 202}. */
    public Map<String, Object> migrateComments(String id, Map<String, Object> body) {
        return http.request("POST", "/v1/documents/" + Http.enc(id) + "/migrate-comments",
                null, body, null, false);
    }

    /** The comment-migration rollup for a document, grouped by migration status. */
    public Map<String, Object> commentMigration(String id) {
        return http.request("GET", "/v1/documents/" + Http.enc(id) + "/comment-migration",
                null, null, null, false);
    }

    /** Download the finished PDF bytes for an unprotected document (resolves + fetches the signed URL). */
    @SuppressWarnings("unchecked")
    public byte[] download(String id) {
        Map<String, Object> doc = get(id);
        Object status = doc.get("status");
        Object downloadObj = doc.get("download");
        Map<String, Object> download = downloadObj instanceof Map ? (Map<String, Object>) downloadObj : null;
        String url = download == null ? null : (String) download.get("url");
        if (!"done".equals(status) || url == null || url.isEmpty()) {
            throw new PageWeaverDocumentFailedException(doc);
        }
        if (Boolean.TRUE.equals(download.get("protected"))) {
            Map<String, Object> annotated = new HashMap<>(doc);
            annotated.put("error", "Document is download-protected; supply a password to download it.");
            throw new PageWeaverDocumentFailedException(annotated);
        }
        return http.fetchUrlBytes(url);
    }

    /** Download a download-protected document by password (the recipient-facing content endpoint). */
    public byte[] download(String id, String password) {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-document-password", password);
        return http.requestBytes("GET", "/v1/documents/" + Http.enc(id) + "/content",
                null, null, headers, true);
    }

    private static boolean isTerminal(Map<String, Object> doc) {
        return TERMINAL.contains(statusOf(doc));
    }

    private static String statusOf(Map<String, Object> doc) {
        Object status = doc == null ? null : doc.get("status");
        return status == null ? "" : String.valueOf(status);
    }

    private static Integer parseIntOrNull(Optional<String> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.get().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
