package io.pageweaver;

import java.util.Map;

/**
 * The content-negotiated result of {@code Documents.createSync}. {@link #kind} distinguishes the raw PDF
 * bytes ({@code "pdf"}), a finished document as JSON ({@code "document"}), or a deadline fallback
 * ({@code "pending"}) whose {@link #id} you then poll.
 */
public final class SyncResult {

    /** One of {@code "pdf"}, {@code "pending"}, {@code "document"}. */
    public final String kind;
    /** Document id (for {@code "pdf"} and {@code "pending"}); may be null. */
    public final String id;
    /** Document version (for {@code "pdf"} and {@code "pending"}); may be null. */
    public final Integer version;
    /** Raw PDF bytes (only when {@code kind == "pdf"}). */
    public final byte[] pdf;
    /** Document status (only when {@code kind == "pending"}). */
    public final String status;
    /** The finished document (only when {@code kind == "document"}). */
    public final Map<String, Object> document;

    private SyncResult(String kind, String id, Integer version, byte[] pdf, String status,
                       Map<String, Object> document) {
        this.kind = kind;
        this.id = id;
        this.version = version;
        this.pdf = pdf;
        this.status = status;
        this.document = document;
    }

    static SyncResult pdf(String id, Integer version, byte[] pdf) {
        return new SyncResult("pdf", id, version, pdf, null, null);
    }

    static SyncResult pending(String id, Integer version, String status) {
        return new SyncResult("pending", id, version, null, status, null);
    }

    static SyncResult document(Map<String, Object> document) {
        return new SyncResult("document", null, null, null, null, document);
    }
}
