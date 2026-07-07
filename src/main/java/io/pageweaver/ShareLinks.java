package io.pageweaver;

import java.util.HashMap;
import java.util.Map;

/**
 * Capability-scoped links that let people without an account view, comment on, or approve a document.
 * Requires a {@code review}-scoped key.
 */
public final class ShareLinks {

    private final Http http;

    ShareLinks(Http http) {
        this.http = http;
    }

    /** Create a share link. The response includes the raw {@code url} and {@code token} exactly once. */
    public Map<String, Object> create(Map<String, Object> body) {
        return http.request("POST", "/v1/share-links", null, body, null, false);
    }

    /** List active + disabled links (never the tokens). Filter by document or review. */
    public Map<String, Object> list(Map<String, Object> params) {
        Map<String, Object> query = new HashMap<>();
        if (params != null) {
            query.put("documentId", params.get("documentId"));
            query.put("reviewRequestId", params.get("reviewRequestId"));
        }
        return http.request("GET", "/v1/share-links", query, null, null, false);
    }

    /** Disable a link immediately (the kill switch). */
    public Map<String, Object> disable(String id) {
        return http.request("POST", "/v1/share-links/" + Http.enc(id) + "/disable", null, null, null, false);
    }
}
