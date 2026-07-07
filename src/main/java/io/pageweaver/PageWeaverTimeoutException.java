package io.pageweaver;

/** {@code waitFor}/{@code createAndWait} exceeded its timeout before the document reached a terminal state. */
public class PageWeaverTimeoutException extends PageWeaverException {

    private final String documentId;
    private final String lastStatus;

    public PageWeaverTimeoutException(String documentId, String lastStatus, long timeoutMs) {
        super("Timed out after " + timeoutMs + "ms waiting for document " + documentId
                + " (last status: " + lastStatus + ").");
        this.documentId = documentId;
        this.lastStatus = lastStatus;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getLastStatus() {
        return lastStatus;
    }
}
