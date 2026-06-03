package com.zhoolg.manage.infrastructure.auth;

import com.zhoolg.manage.exception.ApiException;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoServiceTest {
    @Test
    void decryptsValidRsaOaepCredential() throws Exception {
        KeyPair keyPair = keyPair();
        CryptoService service = new CryptoService(keyPair.getPrivate());

        String credential = "SecureLogin#2026";
        String ciphertext = encrypt(keyPair, credential);

        assertThat(service.decrypt(ciphertext)).isEqualTo(credential);
    }

    @Test
    void rejectsPlaintextOrInvalidCiphertext() throws Exception {
        CryptoService service = new CryptoService(keyPair().getPrivate());

        assertThatThrownBy(() -> service.decrypt("plain-password"))
                .isInstanceOf(ApiException.class)
                .hasMessage("登录凭据格式无效");
    }

    private KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String encrypt(KeyPair keyPair, String text) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        OAEPParameterSpec oaep = new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic(), oaep);
        byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }
}
