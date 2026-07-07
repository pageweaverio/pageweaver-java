package io.pageweaver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Environments &amp; pins: a named per-account pointer set over immutable template versions. Writes require
 * a {@code deploy}-scoped key.
 */
public final class Environments {

    private final Http http;

    Environments(Http http) {
        this.http = http;
    }

    /** Every environment for the account, with pin counts. */
    public List<Object> list() {
        return http.requestList("GET", "/v1/environments", null, null, null, false);
    }

    /** Create a named pointer set (e.g. staging / production). Returns {@code 201}. */
    public Map<String, Object> create(Map<String, Object> body) {
        return http.request("POST", "/v1/environments", null, body, null, false);
    }

    /** Fetch one environment by slug. */
    public Map<String, Object> get(String slug) {
        return http.request("GET", "/v1/environments/" + Http.enc(slug), null, null, null, false);
    }

    /** Rename an environment or flip its production flag. The slug is immutable. */
    public Map<String, Object> update(String slug, Map<String, Object> body) {
        return http.request("PATCH", "/v1/environments/" + Http.enc(slug), null, body, null, false);
    }

    /** Delete an environment and its pins (audited). */
    public Map<String, Object> delete(String slug) {
        return http.request("DELETE", "/v1/environments/" + Http.enc(slug), null, null, null, false);
    }

    /** The template → version pointers in an environment. */
    public List<Object> pins(String slug) {
        return http.requestList("GET", "/v1/environments/" + Http.enc(slug) + "/pins", null, null, null, false);
    }

    /** Point a template at one of its published versions (creates or moves the pin). */
    public Map<String, Object> setPin(String slug, String templateId, int version) {
        Map<String, Object> body = new HashMap<>();
        body.put("version", version);
        return http.request("PUT", "/v1/environments/" + Http.enc(slug) + "/pins/" + Http.enc(templateId),
                null, body, null, false);
    }

    /** Unpin a template from an environment. */
    public Map<String, Object> removePin(String slug, String templateId) {
        return http.request("DELETE", "/v1/environments/" + Http.enc(slug) + "/pins/" + Http.enc(templateId),
                null, null, null, false);
    }

    /** Copy another environment's pin set onto this one (e.g. staging → production). */
    public Map<String, Object> promote(String slug, Map<String, Object> body) {
        return http.request("POST", "/v1/environments/" + Http.enc(slug) + "/promote",
                null, body, null, false);
    }

    /** Roll an environment back to the last successful deployment before the current head. */
    public Map<String, Object> rollback(String slug) {
        return rollback(slug, null);
    }

    /** Roll an environment back to a prior deployment's pin set. Pass {@code toDeploymentId} to target one. */
    public Map<String, Object> rollback(String slug, Map<String, Object> body) {
        return http.request("POST", "/v1/environments/" + Http.enc(slug) + "/rollback",
                null, body, null, false);
    }
}
