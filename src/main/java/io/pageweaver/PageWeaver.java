package io.pageweaver;

/**
 * The PageWeaver API client. Resources are exposed via accessor methods; each returns a cached instance.
 *
 * <pre>{@code
 * PageWeaver pw = new PageWeaver(System.getenv("PAGEWEAVER_API_KEY"));
 * Map<String, Object> doc = pw.documents().createAndWait(
 *     Map.of("templateId", "tmpl_invoice", "payload", Map.of("total", 42)),
 *     new WaitOptions());
 * byte[] pdf = pw.documents().download((String) doc.get("id"));
 * }</pre>
 */
public final class PageWeaver {

    private final Http http;

    private final Documents documents;
    private final Templates templates;
    private final Schemas schemas;
    private final Usage usage;
    private final Comments comments;
    private final Reviews reviews;
    private final ShareLinks shareLinks;
    private final Environments environments;
    private final Deployments deployments;

    /** Create a client against the default base URL ({@code https://api.pageweaver.io}). */
    public PageWeaver(String apiKey) {
        this(apiKey, Http.DEFAULT_BASE_URL);
    }

    /** Create a client against a custom base URL (e.g. {@code http://localhost:4000} in dev). */
    public PageWeaver(String apiKey, String baseUrl) {
        this.http = new Http(apiKey, baseUrl);
        this.documents = new Documents(http);
        this.templates = new Templates(http);
        this.schemas = new Schemas(http);
        this.usage = new Usage(http);
        this.comments = new Comments(http);
        this.reviews = new Reviews(http);
        this.shareLinks = new ShareLinks(http);
        this.environments = new Environments(http);
        this.deployments = new Deployments(http);
    }

    /** Operations on documents: the core of the API. */
    public Documents documents() {
        return documents;
    }

    /** Read-only discovery of templates + pinnable versions; {@code templates().proposals()} for the PR flow. */
    public Templates templates() {
        return templates;
    }

    /** Read-only discovery of the JSON Schemas your payloads validate against. */
    public Schemas schemas() {
        return schemas;
    }

    /** Current-period page usage against the plan quota. */
    public Usage usage() {
        return usage;
    }

    /** Anchored comment threads on documents (requires a {@code review}-scoped key for writes). */
    public Comments comments() {
        return comments;
    }

    /** Review requests + approvals on documents (requires a {@code review}-scoped key for writes). */
    public Reviews reviews() {
        return reviews;
    }

    /** Capability-scoped external share links (requires a {@code review}-scoped key). */
    public ShareLinks shareLinks() {
        return shareLinks;
    }

    /** Named per-account environments + pins over template versions (requires a {@code deploy}-scoped key for writes). */
    public Environments environments() {
        return environments;
    }

    /** Plan documents-as-code deployments from a manifest (requires a {@code deploy}-scoped key for writes). */
    public Deployments deployments() {
        return deployments;
    }
}
