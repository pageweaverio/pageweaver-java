package io.pageweaver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class PageWeaverTest {

    @Test
    void requiresApiKey() {
        assertThrows(IllegalArgumentException.class, () -> new PageWeaver(""));
        assertThrows(IllegalArgumentException.class, () -> new PageWeaver(null));
    }

    @Test
    void wiresAllResourceAccessorsNonNull() {
        PageWeaver pw = new PageWeaver("pk_test_key", "http://localhost:4000");
        assertNotNull(pw.documents());
        assertNotNull(pw.templates());
        assertNotNull(pw.templates().proposals());
        assertNotNull(pw.schemas());
        assertNotNull(pw.usage());
        assertNotNull(pw.comments());
        assertNotNull(pw.reviews());
        assertNotNull(pw.shareLinks());
        assertNotNull(pw.environments());
        assertNotNull(pw.deployments());
    }

    @Test
    void resourceAccessorsAreCached() {
        PageWeaver pw = new PageWeaver("pk_test_key");
        assertEquals(pw.documents(), pw.documents());
        assertEquals(pw.templates(), pw.templates());
        assertEquals(pw.deployments(), pw.deployments());
    }

    @Test
    void exceptionCarriesStatusAndBody() {
        PageWeaverException e = new PageWeaverException("boom", 402, "quota");
        assertEquals(Integer.valueOf(402), e.getStatusCode());
        assertEquals("quota", e.getBody());
    }

    @Test
    void timeoutExceptionCarriesContext() {
        PageWeaverTimeoutException e = new PageWeaverTimeoutException("doc_1", "queued", 60000);
        assertEquals("doc_1", e.getDocumentId());
        assertEquals("queued", e.getLastStatus());
        assertTrue(e instanceof PageWeaverException);
    }

    @Test
    void webhookSignatureRoundTrips() throws Exception {
        String secret = "whsec_abc123";
        String body = "{\"event\":\"document.completed\",\"documentId\":\"doc_1\"}";

        String expected = expectedSignature(secret, body);
        assertEquals(expected, Webhooks.sign(secret, body));

        assertTrue(Webhooks.verifySignature(secret, body, expected));
        assertFalse(Webhooks.verifySignature("wrong_secret", body, expected));
        assertFalse(Webhooks.verifySignature(secret, body, null));
        assertFalse(Webhooks.verifySignature(secret, body, "sha256=deadbeef"));
    }

    @Test
    void verifyWebhookParsesOnValidSignatureAndThrowsOnMismatch() {
        String secret = "whsec_xyz";
        String body = "{\"event\":\"document.failed\",\"documentId\":\"doc_9\"}";
        String sig = Webhooks.sign(secret, body);

        var event = Webhooks.verifyWebhook(secret, body, sig);
        assertEquals("document.failed", event.get("event"));
        assertEquals("doc_9", event.get("documentId"));

        assertThrows(PageWeaverWebhookSignatureException.class,
                () -> Webhooks.verifyWebhook(secret, body, "sha256=00"));
    }

    @Test
    void webhookHeaderConstants() {
        assertEquals("x-pageweaver-signature", Webhooks.SIGNATURE_HEADER);
        assertEquals("x-pageweaver-event", Webhooks.EVENT_HEADER);
        assertEquals("x-pageweaver-timestamp", Webhooks.TIMESTAMP_HEADER);
    }

    /** Independently compute the expected signature with the JDK Mac, to prove Webhooks.sign matches. */
    private static String expectedSignature(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder("sha256=");
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}
