package com.inventory.system.service.ingestion;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class WebhookSignatureVerifier {

    public enum Encoding { BASE64, HEX }

    public boolean verifyHmacSha256(byte[] rawBody, String providedSignature, String secret, Encoding encoding) {
        if (providedSignature == null || secret == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawBody);
            String expected = encoding == Encoding.BASE64
                    ? Base64.getEncoder().encodeToString(digest)
                    : HexFormat.of().formatHex(digest);
            return constantTimeEquals(expected, providedSignature.trim());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        byte[] aa = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aa, bb);
    }
}
