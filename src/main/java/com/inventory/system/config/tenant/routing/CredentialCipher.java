package com.inventory.system.config.tenant.routing;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Envelope encryption for dedicated-tenant DB credentials.
 *
 * <p>AES-256-GCM. The key-encryption key (KEK) is supplied via the environment
 * ({@code MASTERINVENTORY_TENANT_KEK}) and is never persisted. A 256-bit data
 * key is derived from the KEK with SHA-256 so any sufficiently strong passphrase
 * is accepted; for production use a 32-byte random value (base64/hex).
 *
 * <p>Stored form: {@code base64( IV(12 bytes) || ciphertext || GCM-tag(16) )}.
 * The KEK is validated lazily on first use, so the application still boots with
 * the feature disabled and no KEK configured.
 */
@Component
public class CredentialCipher {

    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int IV_LEN = 12;          // 96-bit nonce (GCM standard)
    private static final int TAG_BITS = 128;       // 16-byte auth tag

    private final TenantRoutingProperties props;
    private final SecureRandom random = new SecureRandom();
    private volatile SecretKeySpec cachedKey;

    public CredentialCipher(TenantRoutingProperties props) {
        this.props = props;
    }

    /** Logical id of the KEK used, recorded per row to support rotation. */
    public String keyId() {
        return props.getKeyId();
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        SecretKeySpec key = key(); // KEK-config errors propagate unwrapped
        try {
            byte[] iv = new byte[IV_LEN];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt tenant credential", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        SecretKeySpec key = key(); // KEK-config errors propagate unwrapped
        try {
            byte[] in = Base64.getDecoder().decode(stored);
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(in, 0, iv, 0, IV_LEN);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] pt = cipher.doFinal(in, IV_LEN, in.length - IV_LEN);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Do not leak ciphertext/cause detail.
            throw new IllegalStateException("Failed to decrypt tenant credential (KEK mismatch or tampering)");
        }
    }

    private SecretKeySpec key() {
        SecretKeySpec k = cachedKey;
        if (k != null) {
            return k;
        }
        String kek = props.getKek();
        if (kek == null || kek.isBlank()) {
            throw new IllegalStateException(
                "Tenant credential encryption requested but no KEK configured "
                + "(set MASTERINVENTORY_TENANT_KEK / app.tenant.routing.kek)");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(kek.getBytes(StandardCharsets.UTF_8));
            k = new SecretKeySpec(digest, "AES");
            cachedKey = k;
            return k;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive tenant credential key", e);
        }
    }
}
