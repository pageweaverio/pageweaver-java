package io.pageweaver;

import java.util.HashMap;
import java.util.Map;

/**
 * Anchored comment threads on rendered documents: create, list, reply, and lifecycle
 * (resolve / reopen / close). Requires a {@code review}-scoped key for writes.
 */
public final class Comments {

    private final Http http;

    Comments(Http http) {
        this.http = http;
    }

    /** Create an anchored thread with its first message. Returns {@code 201}. */
    public Map<String, Object> create(Map<String, Object> body) {
        return http.request("POST", "/v1/comments", null, body, null, false);
    }

    /** List a document's threads, newest first. Filter by page/status/severity; page with {@code cursor}. */
    public Map<String, Object> list(String documentId, Map<String, Object> params) {
        Map<String, Object> query = new HashMap<>();
        if (params != null) {
            query.put("pageNumber", params.get("pageNumber"));
            query.put("status", params.get("status"));
            query.put("severity", params.get("severity"));
            query.put("cursor", params.get("cursor"));
            query.put("limit", params.get("limit"));
        }
        return http.request("GET", "/v1/documents/" + Http.enc(documentId) + "/comments",
                query, null, null, false);
    }

    /** Fetch one thread with its full message list. */
    public Map<String, Object> get(String id) {
        return http.request("GET", "/v1/comments/" + Http.enc(id), null, null, null, false);
    }

    /** Edit severity, assignment, due date, or relocate the anchor coordinates. */
    public Map<String, Object> update(String id, Map<String, Object> body) {
        return http.request("PATCH", "/v1/comments/" + Http.enc(id), null, body, null, false);
    }

    /** Reply on a thread. Returns {@code 201}. */
    public Map<String, Object> reply(String id, Map<String, Object> body) {
        return http.request("POST", "/v1/comments/" + Http.enc(id) + "/messages", null, body, null, false);
    }

    /** Resolve a thread (open → resolved). */
    public Map<String, Object> resolve(String id) {
        return http.request("POST", "/v1/comments/" + Http.enc(id) + "/resolve", null, null, null, false);
    }

    /** Reopen a resolved thread (resolved → open). */
    public Map<String, Object> reopen(String id) {
        return http.request("POST", "/v1/comments/" + Http.enc(id) + "/reopen", null, null, null, false);
    }

    /** Close a thread permanently (→ closed, final). */
    public Map<String, Object> close(String id) {
        return http.request("POST", "/v1/comments/" + Http.enc(id) + "/close", null, null, null, false);
    }
}
