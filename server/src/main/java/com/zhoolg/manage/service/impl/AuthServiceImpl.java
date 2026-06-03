package com.zhoolg.manage.service.impl;

import com.zhoolg.manage.exception.ApiErrorCodes;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.service.IAuthService;
import com.zhoolg.manage.infrastructure.auth.CaptchaService;
import com.zhoolg.manage.infrastructure.auth.CryptoService;
import com.zhoolg.manage.infrastructure.auth.LoginAttemptGuardService;
import com.zhoolg.manage.infrastructure.auth.RateLimitService;
import com.zhoolg.manage.entity.base.CurrentUser;
import com.zhoolg.manage.entity.dto.LoginRequestDTO;
import com.zhoolg.manage.entity.dto.LoginResponseDTO;
import com.zhoolg.manage.service.AuditLogService;
import com.zhoolg.manage.service.IPermissionService;
import com.zhoolg.manage.service.UserDirectoryService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Arrays;

/**
 * 认证服务：
 * - 登录凭据由前端经 RSA-OAEP 加密，这里先解密；
 * - 用 Argon2id 校验密码哈希（DB 仅存哈希）；
 * - 校验通过后签发 256 位安全随机会话令牌，服务端持有并带过期时间，
 *   通过 httpOnly + SameSite 的 Cookie 下发，令牌不可被伪造、不可被 JS 读取；
 * - 登录失败只按 IP 阶梯封禁，缓解暴力尝试。
 */
@Service
public class AuthServiceImpl implements IAuthService {

    private record Session(CurrentUser user, Instant expiresAt) {
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    private final CryptoService cryptoService;
    private final PasswordEncoder passwordEncoder;
    private final UserDirectoryService userDirectory;
    private final CaptchaService captchaService;
    private final IPermissionService permissionService;
    private final AuditLogService auditLogService;
    private final RateLimitService rateLimitService;
    private final LoginAttemptGuardService loginAttemptGuardService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.auth.cookie.name:SESSION}")
    private String cookieName;
    @Value("${app.auth.cookie.secure:false}")
    private boolean cookieSecure;
    @Value("${app.auth.cookie.same-site:Strict}")
    private String cookieSameSite;
    @Value("${app.auth.session-ttl-minutes:120}")
    private long sessionTtlMinutes;
    @Value("${app.auth.rate-limit.max-per-minute:30}")
    private int rateLimitPerMinute;
    @Value("${app.auth.session.use-redis:true}")
    private boolean useRedisSession;
    @Value("${app.auth.captcha.enabled:true}")
    private boolean captchaEnabled;
    @Value("${app.auth.seed-password:}")
    private String seedPassword;
    @Value("${spring.profiles.active:${spring.profiles.default:}}")
    private String profiles;

    public AuthServiceImpl(CryptoService cryptoService, PasswordEncoder passwordEncoder, UserDirectoryService userDirectory, CaptchaService captchaService, IPermissionService permissionService, AuditLogService auditLogService, RateLimitService rateLimitService, LoginAttemptGuardService loginAttemptGuardService, ObjectProvider<StringRedisTemplate> redisTemplateProvider, ObjectMapper objectMapper) {
        this.cryptoService = cryptoService;
        this.passwordEncoder = passwordEncoder;
        this.userDirectory = userDirectory;
        this.captchaService = captchaService;
        this.permissionService = permissionService;
        this.auditLogService = auditLogService;
        this.rateLimitService = rateLimitService;
        this.loginAttemptGuardService = loginAttemptGuardService;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void validateProductionSecurityConfig() {
        if (!isProdProfile()) {
            return;
        }
        if (!cookieSecure) {
            throw new IllegalStateException("生产环境必须启用 app.auth.cookie.secure=true");
        }
        if (cookieName == null || !cookieName.startsWith("__Host-")) {
            throw new IllegalStateException("生产环境 Cookie 名称必须使用 __Host- 前缀，例如 __Host-SESSION");
        }
    }

    public LoginResponseDTO login(LoginRequestDTO request, HttpServletResponse response) {
        String accountEncrypted = request.loginName();
        String accountPlain = cryptoService.decrypt(accountEncrypted);
        String clientIp = resolveClientIp(currentRequest());
        String rateKey = "auth:login:" + (accountPlain == null ? "unknown" : accountPlain.trim().toLowerCase());
        if (rateLimitService.overLimit(rateKey, rateLimitPerMinute, Duration.ofMinutes(1))) {
            auditLogService.failure(accountPlain == null ? "unknown" : accountPlain, "auth", "login", "登录过于频繁，请稍后再试");
            throw new ApiException(429, "登录过于频繁，请稍后再试");
        }
        loginAttemptGuardService.ensureAllowed(clientIp);

        // 图形验证码前置网关：未通过则不进行后续解密/校验（降低自动化与暴力攻击成本）
        if (captchaEnabled && !captchaService.verify(request.captchaId(), request.captcha())) {
            auditLogService.failure("anonymous", "auth", "login", "验证码错误或已过期");
            throw new ApiException(400, "验证码错误或已过期");
        }
        String account = accountPlain;
        String password = cryptoService.decrypt(request.password());
        if (account == null || account.isBlank()) {
            throw new ApiException(400, "账号不能为空");
        }
        String key = account.trim().toLowerCase();

        UserDirectoryService.Account acc = userDirectory.findByLoginName(account).orElse(null);
        boolean ok = acc != null
                && acc.enabled()
                && acc.passwordHash() != null
                && passwordEncoder.matches(password, acc.passwordHash());
        if (!ok) {
            loginAttemptGuardService.recordPasswordFailure(clientIp);
            auditLogService.failure(key, "auth", "login", "账号或密码错误");
            // 统一文案，避免账号枚举
            throw new ApiException(401, "账号或密码错误");
        }
        loginAttemptGuardService.resetIpFailures(clientIp);

        return issueLoginResponse(acc, response, "login", "登录成功");
    }

    public LoginResponseDTO loginPasskey(String username, HttpServletResponse response) {
        UserDirectoryService.Account acc = userDirectory.findByLoginName(username)
                .filter(UserDirectoryService.Account::enabled)
                .orElseThrow(() -> new ApiException(401, "Passkey 登录失败"));
        return issueLoginResponse(acc, response, "passkey_login", "Passkey 登录成功");
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        CurrentUser user = null;
        try {
            user = requireUser();
        } catch (ApiException ignored) {
            // 会话可能已经过期，仍清理 Cookie。
        }
        String token = readToken(request);
        if (token != null) {
            sessions.remove(token);
        }
        writeCookie(response, "", Duration.ZERO);
        auditLogService.success(user, "auth", "logout", "user", user == null ? null : user.id(), "退出登录");
    }

    /**
     * 鉴权现已基于 httpOnly Cookie 中的会话令牌。保留 authorization 形参仅为兼容现有
     * 控制器签名，其值被忽略（前端不再发送 Authorization 头）。后续可清理控制器形参。
     */
    public CurrentUser requireUser(String ignoredAuthorization) {
        return requireUser();
    }

    public CurrentUser requireUser() {
        String token = readToken(currentRequest());
        if (token == null) {
            throw new ApiException(ApiErrorCodes.AUTH_SESSION_EXPIRED, "登录已过期，请重新登录");
        }
        Session session = readSession(token);
        if (session == null) {
            throw new ApiException(ApiErrorCodes.AUTH_SESSION_EXPIRED, "登录已过期，请重新登录");
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            if (canUseRedisSession()) {
                deleteRedisSession(token);
            }
            throw new ApiException(ApiErrorCodes.AUTH_SESSION_EXPIRED, "登录已过期，请重新登录");
        }
        UserDirectoryService.Account account = userDirectory.findById(session.user().id()).orElse(null);
        if (account == null || !account.enabled()) {
            sessions.remove(token);
            if (canUseRedisSession()) {
                deleteRedisSession(token);
            }
            throw new ApiException(ApiErrorCodes.AUTH_SESSION_EXPIRED, "账号已失效，请重新登录");
        }
        CurrentUser currentUser = toCurrentUser(account);
        // 续期会话
        Instant newExpiry = Instant.now().plus(Duration.ofMinutes(sessionTtlMinutes));
        sessions.put(token, new Session(currentUser, newExpiry));
        if (canUseRedisSession()) {
            writeRedisSession(token, currentUser, newExpiry);
        }
        return currentUser;
    }

    /**
     * 权限校验（委托给 PermissionServiceImpl）
     */
    public void requirePermission(CurrentUser user, String permission) {
        permissionService.requirePermission(user, permission);
    }

    public long activeSessionCount() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        return sessions.size();
    }

    private String issueSession(CurrentUser user) {
        byte[] raw = new byte[32];
        secureRandom.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(sessionTtlMinutes));
        sessions.put(token, new Session(user, expiresAt));
        if (canUseRedisSession()) {
            writeRedisSession(token, user, expiresAt);
        }
        return token;
    }

    private void writeCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String readToken(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                String value = cookie.getValue();
                return (value == null || value.isBlank()) ? null : value;
            }
        }
        return null;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private CurrentUser toCurrentUser(UserDirectoryService.Account acc) {
        String role = normalizeRole(acc.roleCode());
        List<String> permissions = permissionService.permissionsForRole(role);
        boolean admin = permissions.contains("*");
        return new CurrentUser(
                acc.id(),
                acc.loginName(),
                acc.name() != null ? acc.name() : acc.loginName(),
                role,
                permissions,
                admin
        );
    }

    private String normalizeRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return "viewer";
        }
        String normalized = roleCode.trim().toLowerCase();
        return "admin".equals(normalized) ? "super_admin" : normalized;
    }

    private Session readSession(String token) {
        Session memory = sessions.get(token);
        if (!canUseRedisSession()) {
            return memory;
        }
        try {
            String json = redisTemplate.opsForValue().get(redisKey(token));
            if (json == null) {
                return memory;
            }
            SessionData data = objectMapper.readValue(json, SessionData.class);
            return new Session(data.user(), data.expiresAt());
        } catch (Exception ignored) {
            return memory;
        }
    }

    private void writeRedisSession(String token, CurrentUser user, Instant expiresAt) {
        if (!canUseRedisSession()) {
            return;
        }
        try {
            SessionData data = new SessionData(user, expiresAt);
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(redisKey(token), json, Duration.between(Instant.now(), expiresAt));
        } catch (Exception ignored) {
            // 保留内存会话兜底
        }
    }

    private void deleteRedisSession(String token) {
        if (!canUseRedisSession()) {
            return;
        }
        try {
            redisTemplate.delete(redisKey(token));
        } catch (Exception ignored) {
        }
    }

    private boolean canUseRedisSession() {
        return useRedisSession && redisTemplate != null;
    }

    private boolean isProdProfile() {
        return Arrays.stream((profiles == null ? "" : profiles).split(","))
                .map(String::trim)
                .anyMatch("prod"::equalsIgnoreCase);
    }

    private String redisKey(String token) {
        return "session:" + token;
    }

    private LoginResponseDTO issueLoginResponse(UserDirectoryService.Account acc, HttpServletResponse response, String action, String description) {
        CurrentUser user = toCurrentUser(acc);
        String token = issueSession(user);
        writeCookie(response, token, Duration.ofMinutes(sessionTtlMinutes));
        auditLogService.success(user, "auth", action, "user", user.id(), description);

        // 令牌仅存于 httpOnly Cookie，不再下发到响应体
        return new LoginResponseDTO(
                null,
                new LoginResponseDTO.UserPayload(user.id(), user.username(), user.name(), acc.avatar(), List.of(user.roleCode()), usingDefaultPassword(acc)),
                user.permissions(),
                user.admin(),
                usingDefaultPassword(acc)
        );
    }

    private boolean usingDefaultPassword(UserDirectoryService.Account account) {
        return seedPassword != null
                && !seedPassword.isBlank()
                && account.passwordHash() != null
                && passwordEncoder.matches(seedPassword, account.passwordHash());
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private record SessionData(CurrentUser user, Instant expiresAt) {
    }
}
