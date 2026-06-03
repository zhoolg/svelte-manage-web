package com.zhoolg.manage.infrastructure.auth;

import com.zhoolg.manage.exception.ApiException;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 解密前端使用浏览器 WebCrypto 以 RSA-OAEP(SHA-256) 加密的登录凭据，并导出配对公钥供前端获取。
 *
 * 关键互通点：WebCrypto 的 RSA-OAEP 对 OAEP 摘要与 MGF1 均使用 SHA-256，而 JDK 的
 * "RSA/ECB/OAEPWithSHA-256AndMGF1Padding" 默认 MGF1 为 SHA-1。因此必须显式传入
 * OAEPParameterSpec 将 MGF1 指定为 SHA-256，否则解密必然失败。
 */
@Service
public class CryptoService {
    private final PrivateKey rsaPrivateKey;

    public CryptoService(PrivateKey rsaPrivateKey) {
        this.rsaPrivateKey = rsaPrivateKey;
    }

    /**
     * 解密 Base64 编码的 RSA-OAEP 密文，返回 UTF-8 明文。
     * 登录凭据必须使用当前公钥加密，任何明文或无效密文都直接拒绝。
     */
    public String decrypt(String base64Ciphertext) {
        if (base64Ciphertext == null || base64Ciphertext.isBlank()) {
            throw new ApiException(400, "凭据不能为空");
        }
        try {
            byte[] ciphertext = Base64.getDecoder().decode(base64Ciphertext.trim());
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            OAEPParameterSpec oaep = new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey, oaep);
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new ApiException(400, "登录凭据格式无效");
        }
    }

    /**
     * 导出与私钥配对的 RSA 公钥（SPKI/X.509，Base64）。公钥非机密，供前端获取后做 RSA-OAEP 加密。
     * 配合临时密钥模式：每次启动密钥可能变化，前端应在加密前实时获取。
     */
    public String publicKeyBase64() {
        try {
            if (!(rsaPrivateKey instanceof RSAPrivateCrtKey crt)) {
                throw new ApiException(500, "私钥类型不支持导出公钥");
            }
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
            return Base64.getEncoder().encodeToString(publicKey.getEncoded());
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(500, "公钥导出失败");
        }
    }
}
