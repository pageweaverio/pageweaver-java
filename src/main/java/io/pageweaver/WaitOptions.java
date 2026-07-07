package io.pageweaver;

/** Polling options for {@code Documents.waitFor} / {@code createAndWait}. Mutable, fluent setters. */
public final class WaitOptions {

    /** Initial delay between polls, ms. Default 1000. */
    public long intervalMs = 1000;
    /** Cap the (backing-off) poll delay, ms. Default 5000. */
    public long maxIntervalMs = 5000;
    /** Multiplier applied to the delay after each poll. Default 1.5. */
    public double backoff = 1.5;
    /** Give up after this long, ms. Default 60000. */
    public long timeoutMs = 60000;
    /** Throw {@link PageWeaverDocumentFailedException} if the document fails. Default true. */
    public boolean throwOnFailure = true;

    public WaitOptions() {
    }

    public WaitOptions intervalMs(long v) {
        this.intervalMs = v;
        return this;
    }

    public WaitOptions maxIntervalMs(long v) {
        this.maxIntervalMs = v;
        return this;
    }

    public WaitOptions backoff(double v) {
        this.backoff = v;
        return this;
    }

    public WaitOptions timeoutMs(long v) {
        this.timeoutMs = v;
        return this;
    }

    public WaitOptions throwOnFailure(boolean v) {
        this.throwOnFailure = v;
        return this;
    }
}
