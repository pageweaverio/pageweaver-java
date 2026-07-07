package io.pageweaver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Read-only discovery of your published templates and their pinnable versions. */
public final class Templates {

    private final Http http;
    private final Proposals proposals;

    Templates(Http http) {
        this.http = http;
        this.proposals = new Proposals(http);
    }

    /** Template change proposals — the PR analog for template changes (requires a {@code deploy}-scoped key). */
    public Proposals proposals() {
        return proposals;
    }

    /** All templates owned by the key's account, newest-updated first. */
    public List<Object> list() {
        return http.requestList("GET", "/v1/templates", null, null, null, false);
    }

    /** One template's metadata (name, current version, associated schema, authoring mode). */
    public Map<String, Object> get(String id) {
        return http.request("GET", "/v1/templates/" + Http.enc(id), null, null, null, false);
    }

    /** A template's published version history (newest first). */
    public List<Object> versions(String id) {
        return http.requestList("GET", "/v1/templates/" + Http.enc(id) + "/versions", null, null, null, false);
    }

    /** One published version's metadata. */
    public Map<String, Object> version(String id, int version) {
        return version(id, version, null);
    }

    /**
     * One published version's metadata, plus its frozen editor source when {@code include == "source"}.
     */
    public Map<String, Object> version(String id, int version, String include) {
        Map<String, Object> query = new HashMap<>();
        query.put("include", include);
        return http.request("GET", "/v1/templates/" + Http.enc(id) + "/versions/" + version,
                query, null, null, false);
    }
}
