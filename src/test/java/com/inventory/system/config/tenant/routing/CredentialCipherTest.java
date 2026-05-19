package com.inventory.system.config.tenant.routing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialCipherTest {

    private CredentialCipher cipherWithKek(String kek) {
        TenantRoutingProperties p = new TenantRoutingProperties();
        p.setKek(kek);
        return new CredentialCipher(p);
    }

    @Test
    void roundTrips() {
        CredentialCipher cipher = cipherWithKek("a-strong-test-kek-passphrase");
        String secret = "jdbc:postgresql://db.example:5432/tenant_acme";
        String enc = cipher.encrypt(secret);
        assertThat(enc).isNotEqualTo(secret);
        assertThat(cipher.decrypt(enc)).isEqualTo(secret);
    }

    @Test
    void differentCiphertextEachTime_butSamePlaintext() {
        CredentialCipher cipher = cipherWithKek("kek");
        assertThat(cipher.encrypt("x")).isNotEqualTo(cipher.encrypt("x"));
    }

    @Test
    void tamperedCiphertextFailsClosed() {
        CredentialCipher cipher = cipherWithKek("kek");
        String enc = cipher.encrypt("topsecret");
        String tampered = enc.substring(0, enc.length() - 2) + (enc.endsWith("A") ? "B" : "A") + "=";
        assertThatThrownBy(() -> cipher.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void wrongKekCannotDecrypt() {
        String enc = cipherWithKek("kek-one").encrypt("v");
        assertThatThrownBy(() -> cipherWithKek("kek-two").decrypt(enc))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingKekThrowsOnUse_butConstructionOk() {
        CredentialCipher cipher = cipherWithKek("");
        assertThatThrownBy(() -> cipher.encrypt("v"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KEK");
    }
}
