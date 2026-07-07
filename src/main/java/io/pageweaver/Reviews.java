package io.pageweaver;

import java.util.HashMap;
import java.util.Map;

/**
 * Review requests on documents: create, list, add participants, and collect approvals against a
 * completion policy. Requires a {@code review}-scoped key for writes.
 */
public final class Reviews {

    private final Http http;

    Reviews(Http http) {
        this.http = http;
    }

    /** Open a review on a document with an optional policy + participants. Returns {@code 201}. */
    public Map<String, Object> create(Map<String, Object> body) {
        return http.request("POST", "/v1/reviews", null, body, null, false);
    }

    /** List reviews, newest first. Filter by {@code status}/{@code documentId}; page with {@code cursor}. */
    public Map<String, Object> list(Map<String, Object> params) {
        Map<String, Object> query = new HashMap<>();
        if (params != null) {
            query.put("status", params.get("status"));
            query.put("documentId", params.get("documentId"));
            query.put("cursor", params.get("cursor"));
            query.put("limit", params.get("limit"));
        }
        return http.request("GET", "/v1/reviews", query, null, null, false);
    }

    /** Fetch one review with its participants, approvals, and computed policy state. */
    public Map<String, Object> get(String id) {
        return http.request("GET", "/v1/reviews/" + Http.enc(id), null, null, null, false);
    }

    /** Add a participant (member {@code userId}, or {@code externalEmail} + {@code externalName}) with a role. */
    public Map<String, Object> addParticipant(String id, Map<String, Object> body) {
        return http.request("POST", "/v1/reviews/" + Http.enc(id) + "/participants", null, body, null, false);
    }

    /** Record an approval decision. Returns {@code 201}; the review auto-completes when its policy is met. */
    public Map<String, Object> approve(String id, Map<String, Object> body) {
        return http.request("POST", "/v1/reviews/" + Http.enc(id) + "/approvals", null, body, null, false);
    }

    /** Manually complete a review (policy-satisfied, or forced by an admin). */
    public Map<String, Object> complete(String id) {
        return http.request("POST", "/v1/reviews/" + Http.enc(id) + "/complete", null, null, null, false);
    }

    /** Withdraw a review (open → canceled). */
    public Map<String, Object> cancel(String id) {
        return http.request("POST", "/v1/reviews/" + Http.enc(id) + "/cancel", null, null, null, false);
    }
}
