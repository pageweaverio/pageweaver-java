package io.pageweaver;

/** Thrown when a webhook signature does not match the body. */
public class PageWeaverWebhookSignatureException extends PageWeaverException {

    public PageWeaverWebhookSignatureException() {
        super("Invalid webhook signature.");
    }
}
