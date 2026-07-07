package io.pageweaver;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verify inbound PageWeaver webhook signatures. Each delivery is signed with HMAC-SHA256 over the exact
 * request body, keyed by your account webhook secret ({@code whsec_...}), sent in the
 * {@code X-PageWeaver-Signature} header formatted {@code sha256=<hex>}. Verify it before trusting the payload.
 */
public final class Webhooks {

    /** Header carrying the {@code sha256=<hex>} signature. */
    public static final String SIGNATURE_HEADER = "x-pageweaver-signature";
    /** Header carrying the event name. */
    public static final String EVENT_HEADER = "x-pageweaver-event";
    /** Header carrying the unix-seconds send time. */
    public static final String TIMESTAMP_HEADER = "x-pageweaver-timestamp";

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Webhooks() {
    }

    /** Compute the {@code sha256=<hex>} signature for a body. Exposed mainly for tests. */
    public static String sign(String secret, String rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + toHex(digest);
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            throw new PageWeaverException("Failed to compute webhook signature: " + e.getMessage());
        }
    }

    /** Constant-time check of a {@code sha256=<hex>} signature against the raw body. Never throws. */
    public static boolean verifySignature(String secret, String body, String signature) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }
        byte[] expected = sign(secret, body).getBytes(StandardCharsets.UTF_8);
        byte[] actual = signature.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    /**
     * Verify a webhook signature and return the parsed event. Throws
     * {@link PageWeaverWebhookSignatureException} if the signature is missing or wrong.
     */
    public static Map<String, Object> verifyWebhook(String secret, String body, String signature) {
        if (!verifySignature(secret, body, signature)) {
            throw new PageWeaverWebhookSignatureException();
        }
        Map<String, Object> parsed = GSON.fromJson(body, MAP_TYPE);
        return parsed == null ? Map.of() : parsed;
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }
}
