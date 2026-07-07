package io.pageweaver;

import java.util.Map;

/**
 * The document reached the terminal {@code failed} state while waiting, or a download could not proceed.
 * Thrown by {@code waitFor}/{@code createAndWait} unless {@code throwOnFailure} is false. {@link #getDocument()}
 * carries the final response (including its {@code error} string).
 */
public class PageWeaverDocumentFailedException extends PageWeaverException {

    private final transient Map<String, Object> document;

    public PageWeaverDocumentFailedException(Map<String, Object> document) {
        super(buildMessage(document));
        this.document = document;
    }

    private static String buildMessage(Map<String, Object> document) {
        Object id = document == null ? null : document.get("id");
        Object err = document == null ? null : document.get("error");
        return "Document " + id + " failed: " + (err != null ? err : "unknown error");
    }

    public Map<String, Object> getDocument() {
        return document;
    }
}
