package io.pageweaver;

import java.util.Map;

/** Your page consumption against the plan quota for the current billing period. */
public final class Usage {

    private final Http http;

    Usage(Http http) {
        this.http = http;
    }

    /** Current-period usage: billable document pages and editor preview pages, with their limits. */
    public Map<String, Object> get() {
        return http.request("GET", "/v1/usage", null, null, null, false);
    }
}
