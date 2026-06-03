package com.zhoolg.manage.infrastructure.auth;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretCryptoServiceTest {

    @Test
    void roundTripsSecretInDevelopmentProfile() {
        SecretCryptoService service = new SecretCryptoService("unit-test-secret");
        String encrypted = service.encrypt("sk-test");

        assertThat(service.decrypt(encrypted)).isEqualTo("sk-test");
    }

    @Test
    void rejectsDefaultSecretInProductionProfile() {
        SecretCryptoService service = new SecretCryptoService("dev-only-change-me-32-byte-secret");
        ReflectionTestUtils.setField(service, "profiles", "prod");

        assertThatThrownBy(service::validateProductionSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.ai.credentials.secret");
    }
}
