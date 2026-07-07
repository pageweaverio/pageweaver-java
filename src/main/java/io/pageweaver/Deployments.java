package io.pageweaver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deployments — documents-as-code. Plan a {@code pageweaver.yml} manifest against a target environment,
 * then apply it. Plan and apply are separate, explicit calls. Writes require a {@code deploy}-scoped key.
 */
public final class Deployments {

    private final Http http;

    Deployments(Http http) {
        this.http = http;
    }

    /** Plan a deployment: re-validate the manifest, diff against live state, return a change list ({@code 202}). */
    public Map<String, Object> plan(Map<String, Object> body) {
        return http.request("POST", "/v1/deployments/plan", null, body, null, false);
    }

    /** Plan a deployment with an idempotency key (re-planning returns the existing plan). */
    public Map<String, Object> plan(Map<String, Object> body, String idempotencyKey) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Idempotency-Key", idempotencyKey);
        return http.request("POST", "/v1/deployments/plan", null, body, headers, false);
    }

    /** Recent deployments for the account, newest first. Filter by {@code environment} slug. */
    public List<Object> list(Map<String, Object> params) {
        Map<String, Object> query = new HashMap<>();
        if (params != null) {
            query.put("environment", params.get("environment"));
            query.put("limit", params.get("limit"));
        }
        return http.requestList("GET", "/v1/deployments", query, null, null, false);
    }

    /** One deployment with its per-resource plan lines and their apply outcomes. */
    public Map<String, Object> get(String id) {
        return http.request("GET", "/v1/deployments/" + Http.enc(id), null, null, null, false);
    }

    /** Apply a planned deployment: publish the changed versions and write the environment's pins ({@code 202}). */
    public Map<String, Object> apply(String id) {
        return http.request("POST", "/v1/deployments/" + Http.enc(id) + "/apply", null, null, null, false);
    }
}
