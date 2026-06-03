package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.base.CurrentUser;
import com.zhoolg.manage.entity.dto.ChangePasswordDTO;
import com.zhoolg.manage.entity.dto.UpdateProfileDTO;
import com.zhoolg.manage.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileServiceTest {

    @Test
    void updatesCurrentUserProfile() {
        IAuthService authService = mock(IAuthService.class);
        UserDirectoryService userDirectory = mock(UserDirectoryService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        ProfileService service = new ProfileService(authService, userDirectory, passwordEncoder, auditLogService);
        CurrentUser current = new CurrentUser(1L, "admin", "旧名称", "super_admin", List.of("*"), true);
        UserDirectoryService.Account account = account("旧名称", "hash");
        when(authService.requireUser()).thenReturn(current);
        when(userDirectory.findById(1L)).thenReturn(Optional.of(account));

        var payload = service.updateProfile(new UpdateProfileDTO(" 新名称 ", "https://example.com/avatar.png"));

        assertThat(payload.name()).isEqualTo("新名称");
        assertThat(payload.avatar()).isEqualTo("https://example.com/avatar.png");
        verify(userDirectory).update(any(UserDirectoryService.Account.class));
        verify(auditLogService).success(eq(current), eq("auth"), eq("update_profile"), eq("user"), eq(1L), eq("更新个人资料"), any());
    }

    @Test
    void changesPasswordWhenOldPasswordMatches() {
        IAuthService authService = mock(IAuthService.class);
        UserDirectoryService userDirectory = mock(UserDirectoryService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        ProfileService service = new ProfileService(authService, userDirectory, passwordEncoder, auditLogService);
        CurrentUser current = new CurrentUser(1L, "admin", "管理员", "super_admin", List.of("*"), true);
        when(authService.requireUser()).thenReturn(current);
        when(userDirectory.findById(1L)).thenReturn(Optional.of(account("管理员", "old-hash")));
        when(passwordEncoder.matches("old-pass", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-pass", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");

        service.changePassword(new ChangePasswordDTO("old-pass", "new-pass", "new-pass"));

        verify(userDirectory).savePasswordHash(1L, "new-hash");
        verify(auditLogService).success(current, "auth", "change_password", "user", 1L, "修改密码");
    }

    @Test
    void rejectsWrongOldPassword() {
        IAuthService authService = mock(IAuthService.class);
        UserDirectoryService userDirectory = mock(UserDirectoryService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        ProfileService service = new ProfileService(authService, userDirectory, passwordEncoder, auditLogService);
        CurrentUser current = new CurrentUser(1L, "admin", "管理员", "super_admin", List.of("*"), true);
        when(authService.requireUser()).thenReturn(current);
        when(userDirectory.findById(1L)).thenReturn(Optional.of(account("管理员", "old-hash")));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordDTO("wrong", "new-pass", "new-pass")))
                .isInstanceOf(ApiException.class)
                .hasMessage("旧密码错误");
    }

    private UserDirectoryService.Account account(String name, String passwordHash) {
        return new UserDirectoryService.Account(1L, "admin", passwordHash, name, null, "super_admin", true, null, null);
    }
}
