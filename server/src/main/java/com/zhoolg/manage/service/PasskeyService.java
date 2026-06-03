package com.zhoolg.manage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.UserIdentity;
import com.zhoolg.manage.cache.CacheService;
import com.zhoolg.manage.entity.dto.LoginResponseDTO;
import com.zhoolg.manage.entity.dto.PasskeyLoginFinishDTO;
import com.zhoolg.manage.entity.dto.PasskeyLoginStartDTO;
import com.zhoolg.manage.entity.dto.PasskeyCredentialDTO;
import com.zhoolg.manage.entity.dto.PasskeyRegisterFinishDTO;
import com.zhoolg.manage.entity.dto.PasskeyStartResponseDTO;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.infrastructure.auth.AdminPasskeyRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PasskeyService {

    private final AdminPasskeyRepository passkeyRepository;
    private final UserDirectoryService userDirectory;
    private final IAuthService authService;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    private final SystemSettingService systemSettingService;

    @Value("${app.auth.passkey.rp-id:localhost}")
    private String relyingPartyId;
    @Value("${app.auth.passkey.rp-name:Manage Admin}")
    private String relyingPartyName;
    @Value("${app.auth.passkey.origins:http://localhost:7052,http://localhost:5173,http://localhost:8080}")
    private String allowedOrigins;
    @Value("${app.auth.passkey.challenge-ttl-minutes:5}")
    private long challengeTtlMinutes;

    public PasskeyService(AdminPasskeyRepository passkeyRepository, UserDirectoryService userDirectory, IAuthService authService, CacheService cacheService, ObjectMapper objectMapper, SystemSettingService systemSettingService) {
        this.passkeyRepository = passkeyRepository;
        this.userDirectory = userDirectory;
        this.authService = authService;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
        this.systemSettingService = systemSettingService;
    }

    public PasskeyStartResponseDTO startRegistration() {
        var current = authService.requireUser();
        var account = userDirectory.findById(current.id())
                .orElseThrow(() -> new ApiException(401, "账号已失效，请重新登录"));
        UserIdentity user = UserIdentity.builder()
                .name(account.loginName())
                .displayName(account.name() == null || account.name().isBlank() ? account.loginName() : account.name())
                .id(passkeyRepository.userHandle(account.id()))
                .build();
        try {
            PublicKeyCredentialCreationOptions options = relyingParty().startRegistration(
                    StartRegistrationOptions.builder()
                            .user(user)
                            .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                                    .residentKey(ResidentKeyRequirement.REQUIRED)
                                    .userVerification(UserVerificationRequirement.PREFERRED)
                                    .build())
                            .build()
            );
            String requestId = UUID.randomUUID().toString();
            cacheService.set(registrationKey(requestId), options.toJson(), Duration.ofMinutes(challengeTtlMinutes));
            return new PasskeyStartResponseDTO(requestId, publicKeyNode(options.toCredentialsCreateJson()));
        } catch (Exception ex) {
            throw new ApiException(500, "创建 Passkey 注册挑战失败");
        }
    }

    public void finishRegistration(PasskeyRegisterFinishDTO request) {
        String optionsJson = cacheService.get(registrationKey(request.requestId()))
                .orElseThrow(() -> new ApiException(400, "Passkey 注册挑战已过期"));
        var current = authService.requireUser();
        var account = userDirectory.findById(current.id())
                .orElseThrow(() -> new ApiException(401, "账号已失效，请重新登录"));
        try {
            RegistrationResult result = relyingParty().finishRegistration(
                    FinishRegistrationOptions.builder()
                            .request(PublicKeyCredentialCreationOptions.fromJson(optionsJson))
                            .response(PublicKeyCredential.parseRegistrationResponseJson(request.credentialJson()))
                            .build()
            );
            passkeyRepository.saveCredential(
                    account.id(),
                    account.loginName(),
                    result.getKeyId().getId().getBase64Url(),
                    result.getPublicKeyCose().getBase64Url(),
                    result.getSignatureCount(),
                    request.displayName()
            );
            cacheService.delete(registrationKey(request.requestId()));
        } catch (Exception ex) {
            throw new ApiException(400, "Passkey 注册失败");
        }
    }

    public List<PasskeyCredentialDTO> listCurrentUserPasskeys() {
        var current = authService.requireUser();
        return passkeyRepository.listByUserId(current.id()).stream()
                .map(row -> new PasskeyCredentialDTO(
                        row.id(),
                        normalizeDisplayName(row.displayName()),
                        row.username(),
                        row.lastUsedTime(),
                        row.createTime()
                ))
                .toList();
    }

    public void deleteCurrentUserPasskey(long id) {
        var current = authService.requireUser();
        if (!passkeyRepository.disableForUser(id, current.id())) {
            throw new ApiException(404, "Passkey 不存在或已删除");
        }
    }

    public PasskeyStartResponseDTO startLogin(PasskeyLoginStartDTO request) {
        try {
            StartAssertionOptions.StartAssertionOptionsBuilder builder = StartAssertionOptions.builder();
            if (request != null && request.username() != null && !request.username().isBlank()) {
                builder.username(request.username().trim());
            }
            AssertionRequest assertionRequest = relyingParty().startAssertion(builder.build());
            String requestId = UUID.randomUUID().toString();
            cacheService.set(assertionKey(requestId), assertionRequest.toJson(), Duration.ofMinutes(challengeTtlMinutes));
            return new PasskeyStartResponseDTO(requestId, publicKeyNode(assertionRequest.toCredentialsGetJson()));
        } catch (Exception ex) {
            throw new ApiException(400, "创建 Passkey 登录挑战失败");
        }
    }

    public LoginResponseDTO finishLogin(PasskeyLoginFinishDTO request, HttpServletResponse response) {
        String assertionJson = cacheService.get(assertionKey(request.requestId()))
                .orElseThrow(() -> new ApiException(400, "Passkey 登录挑战已过期"));
        try {
            AssertionResult result = relyingParty().finishAssertion(
                    FinishAssertionOptions.builder()
                            .request(AssertionRequest.fromJson(assertionJson))
                            .response(PublicKeyCredential.parseAssertionResponseJson(request.credentialJson()))
                            .build()
            );
            passkeyRepository.updateSignatureCount(result.getCredentialId().getBase64Url(), result.getSignatureCount());
            cacheService.delete(assertionKey(request.requestId()));
            return authService.loginPasskey(result.getUsername(), response);
        } catch (Exception ex) {
            throw new ApiException(401, "Passkey 登录失败");
        }
    }

    private RelyingParty relyingParty() {
        String rpId = systemSettingService.getSetting("auth.passkey-rp-id", relyingPartyId);
        String rpName = systemSettingService.getSetting("auth.passkey-rp-name", relyingPartyName);

        return RelyingParty.builder()
                .identity(RelyingPartyIdentity.builder().id(rpId).name(rpName).build())
                .credentialRepository(passkeyRepository)
                .origins(origins())
                .allowOriginPort(true)
                .allowUntrustedAttestation(true)
                .build();
    }

    private Set<String> origins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toSet());
    }

    private JsonNode publicKeyNode(String credentialOptionsJson) throws Exception {
        JsonNode root = objectMapper.readTree(credentialOptionsJson);
        return root.has("publicKey") ? root.get("publicKey") : root;
    }

    private String normalizeDisplayName(String displayName) {
        return displayName == null || displayName.isBlank() ? "Passkey" : displayName.trim();
    }

    private String registrationKey(String requestId) {
        return "auth:passkey:registration:" + requestId;
    }

    private String assertionKey(String requestId) {
        return "auth:passkey:assertion:" + requestId;
    }
}
