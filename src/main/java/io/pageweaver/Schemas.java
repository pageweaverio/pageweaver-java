package io.pageweaver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Read-only discovery of the JSON Schemas your payloads validate against. */
public final class Schemas {

    private final Http http;

    Schemas(Http http) {
        this.http = http;
    }

    /** All schemas owned by the key's account, newest-updated first. */
    public List<Object> list() {
        return http.requestList("GET", "/v1/schemas", null, null, null, false);
    }

    /** A schema's published JSON Schema for the latest published version. */
    public Map<String, Object> get(String id) {
        return http.request("GET", "/v1/schemas/" + Http.enc(id), null, null, null, false);
    }

    /** A schema's published JSON Schema for a specific version. */
    public Map<String, Object> get(String id, int version) {
        Map<String, Object> query = new HashMap<>();
        query.put("version", version);
        return http.request("GET", "/v1/schemas/" + Http.enc(id), query, null, null, false);
    }

    /** A schema's published version history (newest first). */
    public List<Object> versions(String id) {
        return http.requestList("GET", "/v1/schemas/" + Http.enc(id) + "/versions", null, null, null, false);
    }

    /** One published version's metadata. */
    public Map<String, Object> version(String id, int version) {
        return version(id, version, null);
    }

    /** One published version's metadata, plus its frozen FieldNode tree when {@code include == "nodes"}. */
    public Map<String, Object> version(String id, int version, String include) {
        Map<String, Object> query = new HashMap<>();
        query.put("include", include);
        return http.request("GET", "/v1/schemas/" + Http.enc(id) + "/versions/" + version,
                query, null, null, false);
    }
}
