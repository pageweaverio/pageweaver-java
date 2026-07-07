package io.pageweaver;

/** Raised when the API returns a non-2xx response, or the request fails. Base for all SDK errors. */
public class PageWeaverException extends RuntimeException {

    private final Integer statusCode;
    private final String body;

    public PageWeaverException(String message) {
        this(message, null, null);
    }

    public PageWeaverException(String message, Integer statusCode, String body) {
        super(message);
        this.statusCode = statusCode;
        this.body = body;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }
}
