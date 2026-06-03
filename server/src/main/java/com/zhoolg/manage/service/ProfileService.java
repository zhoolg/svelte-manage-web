package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.ChangePasswordDTO;
import com.zhoolg.manage.entity.dto.LoginResponseDTO;
import com.zhoolg.manage.entity.dto.UpdateProfileDTO;
import com.zhoolg.manage.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 当前登录用户资料服务：负责用户中心中的资料更新与密码修改。
 */
@Service
public class ProfileService {
    private final IAuthService authService;
    private final UserDirectoryService userDirectory;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Value("${app.auth.seed-password:}")
    private String seedPassword;

    public ProfileService(IAuthService authService, UserDirectoryService userDirectory, PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.authService = authService;
        this.userDirectory = userDirectory;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public LoginResponseDTO.UserPayload currentUser() {
        var current = authService.requireUser();
        return userDirectory.findById(current.id())
                .map(this::toPayload)
                .orElseThrow(() -> new ApiException(404, "账号不存在"));
    }

    @Transactional
    public LoginResponseDTO.UserPayload updateProfile(UpdateProfileDTO request) {
        var current = authService.requireUser();
        UserDirectoryService.Account account = userDirectory.findById(current.id())
                .orElseThrow(() -> new ApiException(404, "账号不存在"));
        String nextName = request.name() == null ? "" : request.name().trim();
        if (nextName.isBlank()) {
            throw new ApiException(400, "姓名不能为空");
        }
        String nextAvatar = normalizeAvatar(request.avatar());

        UserDirectoryService.Account updated = new UserDirectoryService.Account(
                account.id(),
                account.loginName(),
                account.passwordHash(),
                nextName,
                nextAvatar,
                account.roleCode(),
                account.enabled(),
                account.createTime(),
                account.updateTime()
        );
        userDirectory.update(updated);
        auditLogService.success(current, "auth", "update_profile", "user", current.id(), "更新个人资料", Map.of("name", nextName, "avatarUpdated", nextAvatar != null));
        return toPayload(updated);
    }

    @Transactional
    public void changePassword(ChangePasswordDTO request) {
        var current = authService.requireUser();
        UserDirectoryService.Account account = userDirectory.findById(current.id())
                .orElseThrow(() -> new ApiException(404, "账号不存在"));
        if (request.newPassword() == null || !request.newPassword().equals(request.confirmPassword())) {
            throw new ApiException(400, "两次输入的新密码不一致");
        }
        if (request.oldPassword() == null
                || account.passwordHash() == null
                || !passwordEncoder.matches(request.oldPassword(), account.passwordHash())) {
            auditLogService.failure(current.username(), "auth", "change_password", "旧密码错误");
            throw new ApiException(400, "旧密码错误");
        }
        if (passwordEncoder.matches(request.newPassword(), account.passwordHash())) {
            throw new ApiException(400, "新密码不能与旧密码相同");
        }

        userDirectory.savePasswordHash(account.id(), passwordEncoder.encode(request.newPassword()));
        auditLogService.success(current, "auth", "change_password", "user", current.id(), "修改密码");
    }

    private LoginResponseDTO.UserPayload toPayload(UserDirectoryService.Account account) {
        return new LoginResponseDTO.UserPayload(
                account.id(),
                account.loginName(),
                account.name() == null || account.name().isBlank() ? account.loginName() : account.name(),
                account.avatar(),
                List.of(normalizeRole(account.roleCode())),
                usingDefaultPassword(account)
        );
    }

    private boolean usingDefaultPassword(UserDirectoryService.Account account) {
        return seedPassword != null
                && !seedPassword.isBlank()
                && account.passwordHash() != null
                && passwordEncoder.matches(seedPassword, account.passwordHash());
    }

    private String normalizeRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return "viewer";
        }
        String normalized = roleCode.trim().toLowerCase();
        return "admin".equals(normalized) ? "super_admin" : normalized;
    }

    private String normalizeAvatar(String avatar) {
        return avatar == null || avatar.isBlank() ? null : avatar.trim();
    }
}
