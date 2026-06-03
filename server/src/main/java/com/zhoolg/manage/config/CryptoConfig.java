package com.zhoolg.manage.config;

import com.zhoolg.manage.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * 加密相关 Bean：
 * - Argon2id 密码编码器；
 * - RSA 私钥：生产环境从外部密钥文件加载；开发环境未配置时生成临时密钥。
 */
@Configuration
public class CryptoConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    }

    @Bean
    public PrivateKey rsaPrivateKey(
            @Value("${app.crypto.private-key-location:}") Resource location,
            @Value("${app.crypto.generate-ephemeral-key:false}") boolean generateEphemeralKey
    ) {
        if (location != null && location.exists()) {
            return loadPrivateKey(location);
        }
        if (generateEphemeralKey) {
            return generatePrivateKey();
        }
        throw new ApiException(500, "RSA 私钥未配置");
    }

    private PrivateKey loadPrivateKey(Resource location) {
        try (InputStream in = location.getInputStream()) {
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String beginMarker = "-----BEGIN " + "PRIVATE KEY-----";
            String endMarker = "-----END " + "PRIVATE KEY-----";
            String body = pem
                    .replace(beginMarker, "")
                    .replace(endMarker, "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(body);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception ex) {
            throw new ApiException(500, "RSA 私钥加载失败");
        }
    }

    private PrivateKey generatePrivateKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair().getPrivate();
        } catch (Exception ex) {
            throw new ApiException(500, "RSA 临时密钥生成失败");
        }
    }
}
