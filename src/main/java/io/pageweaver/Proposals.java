package io.pageweaver;

import java.util.HashMap;
import java.util.Map;

/**
 * Template proposals — the PR analog for template changes. Reached as {@code templates().proposals()},
 * scoped to a template id passed on each call. Writes require a {@code deploy}-scoped key.
 */
public final class Proposals {

    private final Http http;

    Proposals(Http http) {
        this.http = http;
    }

    private String base(String templateId) {
        return "/v1/templates/" + Http.enc(templateId) + "/proposals";
    }

    /** Open a proposal on a template: freeze a candidate. Returns {@code 202}. */
    public Map<String, Object> open(String templateId, Map<String, Object> body) {
        return http.request("POST", base(templateId), null, body, null, false);
    }

    /** List a template's proposals, newest first. Filter by {@code status}; page with {@code cursor}. */
    public Map<String, Object> list(String templateId, Map<String, Object> params) {
        Map<String, Object> query = new HashMap<>();
        if (params != null) {
            query.put("status", params.get("status"));
            query.put("cursor", params.get("cursor"));
            query.put("limit", params.get("limit"));
        }
        return http.request("GET", base(templateId), query, null, null, false);
    }

    /** Fetch one proposal with its check summary, approvals, and promote-gate state. */
    public Map<String, Object> get(String templateId, String proposalId) {
        return http.request("GET", base(templateId) + "/" + Http.enc(proposalId), null, null, null, false);
    }

    /** Re-run the render-diff regression. Returns {@code 202}. */
    public Map<String, Object> rerunChecks(String templateId, String proposalId) {
        return http.request("POST", base(templateId) + "/" + Http.enc(proposalId) + "/checks",
                null, null, null, false);
    }

    /** Append an approval decision. Returns {@code 201}. */
    public Map<String, Object> approve(String templateId, String proposalId, Map<String, Object> body) {
        return http.request("POST", base(templateId) + "/" + Http.enc(proposalId) + "/approve",
                null, body, null, false);
    }

    /** Append a rejection decision. Returns {@code 201}. */
    public Map<String, Object> reject(String templateId, String proposalId, Map<String, Object> body) {
        return http.request("POST", base(templateId) + "/" + Http.enc(proposalId) + "/reject",
                null, body, null, false);
    }

    /** Promote the candidate: publish it as the next version. Fails ({@code 409}) if the gate is unmet. */
    public Map<String, Object> promote(String templateId, String proposalId) {
        return http.request("POST", base(templateId) + "/" + Http.enc(proposalId) + "/promote",
                null, null, null, false);
    }

    /** Withdraw an open proposal (only while open). The live version is untouched. */
    public Map<String, Object> retract(String templateId, String proposalId) {
        return http.request("DELETE", base(templateId) + "/" + Http.enc(proposalId), null, null, null, false);
    }
}
