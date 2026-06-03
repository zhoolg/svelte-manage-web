package com.zhoolg.manage.controller.admin;

import com.zhoolg.manage.infrastructure.auth.CaptchaService;
import com.zhoolg.manage.infrastructure.auth.CryptoService;
import com.zhoolg.manage.entity.base.ApiResponse;
import com.zhoolg.manage.entity.dto.ChangePasswordDTO;
import com.zhoolg.manage.entity.dto.LoginRequestDTO;
import com.zhoolg.manage.entity.dto.LoginResponseDTO;
import com.zhoolg.manage.entity.dto.PasskeyCredentialDTO;
import com.zhoolg.manage.entity.dto.PasskeyLoginFinishDTO;
import com.zhoolg.manage.entity.dto.PasskeyLoginStartDTO;
import com.zhoolg.manage.entity.dto.PasskeyRegisterFinishDTO;
import com.zhoolg.manage.entity.dto.PasskeyStartResponseDTO;
import com.zhoolg.manage.entity.dto.UpdateProfileDTO;
import com.zhoolg.manage.service.IAuthService;
import com.zhoolg.manage.service.PasskeyService;
import com.zhoolg.manage.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/auth")
public class AuthController {
    private final IAuthService authService;
    private final ProfileService profileService;
    private final PasskeyService passkeyService;
    private final CryptoService cryptoService;
    private final CaptchaService captchaService;

    public AuthController(IAuthService authService, ProfileService profileService, PasskeyService passkeyService, CryptoService cryptoService, CaptchaService captchaService) {
        this.authService = authService;
        this.profileService = profileService;
        this.passkeyService = passkeyService;
        this.cryptoService = cryptoService;
        this.captchaService = captchaService;
    }

    /** 图形验证码：返回 {captchaId, image(dataURL)}，答案仅存服务端。no-store 防缓存。 */
    @GetMapping("/captcha")
    public ApiResponse<CaptchaService.Captcha> captcha(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return ApiResponse.ok(captchaService.generate());
    }

    /**
     * 返回当前 RSA 公钥（Base64 SPKI），供前端在加密登录凭据前实时获取。公钥非机密，无需鉴权。
     */
    @GetMapping("/public-key")
    public ApiResponse<Map<String, String>> publicKey() {
        return ApiResponse.ok(Map.of("publicKey", cryptoService.publicKeyBase64()));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request, HttpServletResponse response) {
        return ApiResponse.ok(authService.login(request, response), "登录成功");
    }

    @PostMapping("/passkeys/registration/options")
    public ApiResponse<PasskeyStartResponseDTO> passkeyRegistrationOptions() {
        return ApiResponse.ok(passkeyService.startRegistration());
    }

    @PostMapping("/passkeys/registration/finish")
    public ApiResponse<Map<String, Boolean>> passkeyRegistrationFinish(@Valid @RequestBody PasskeyRegisterFinishDTO request) {
        passkeyService.finishRegistration(request);
        return ApiResponse.ok(Map.of("success", true), "Passkey 已绑定");
    }

    @GetMapping("/passkeys")
    public ApiResponse<List<PasskeyCredentialDTO>> passkeys() {
        return ApiResponse.ok(passkeyService.listCurrentUserPasskeys());
    }

    @DeleteMapping("/passkeys/{id}")
    public ApiResponse<Map<String, Boolean>> deletePasskey(@PathVariable long id) {
        passkeyService.deleteCurrentUserPasskey(id);
        return ApiResponse.ok(Map.of("success", true), "Passkey 已删除");
    }

    @PostMapping("/passkeys/assertion/options")
    public ApiResponse<PasskeyStartResponseDTO> passkeyAssertionOptions(@RequestBody(required = false) PasskeyLoginStartDTO request) {
        return ApiResponse.ok(passkeyService.startLogin(request));
    }

    @PostMapping("/passkeys/assertion/finish")
    public ApiResponse<LoginResponseDTO> passkeyAssertionFinish(@Valid @RequestBody PasskeyLoginFinishDTO request, HttpServletResponse response) {
        return ApiResponse.ok(passkeyService.finishLogin(request, response), "登录成功");
    }

    @GetMapping("/me")
    public ApiResponse<LoginResponseDTO.UserPayload> me() {
        return ApiResponse.ok(profileService.currentUser());
    }

    @PutMapping("/profile")
    public ApiResponse<LoginResponseDTO.UserPayload> updateProfile(@Valid @RequestBody UpdateProfileDTO request) {
        return ApiResponse.ok(profileService.updateProfile(request), "资料已更新");
    }

    @PutMapping("/password")
    public ApiResponse<Map<String, Boolean>> changePassword(@Valid @RequestBody ChangePasswordDTO request) {
        profileService.changePassword(request);
        return ApiResponse.ok(Map.of("success", true), "密码已修改");
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, Boolean>> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ApiResponse.ok(Map.of("success", true));
    }
}
