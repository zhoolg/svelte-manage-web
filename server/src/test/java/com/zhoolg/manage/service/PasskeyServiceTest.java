package com.zhoolg.manage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.entity.base.CurrentUser;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.infrastructure.auth.AdminPasskeyRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasskeyServiceTest {

    @Test
    void listsCurrentUserPasskeys() {
        AdminPasskeyRepository repository = mock(AdminPasskeyRepository.class);
        IAuthService authService = mock(IAuthService.class);
        SystemSettingService settingService = mock(SystemSettingService.class);
        PasskeyService service = new PasskeyService(repository, null, authService, null, new ObjectMapper(), settingService);
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 2, 10, 0);
        LocalDateTime lastUsedAt = LocalDateTime.of(2026, 6, 2, 12, 30);
        when(authService.requireUser()).thenReturn(currentUser());
        when(repository.listByUserId(1L)).thenReturn(List.of(
                new AdminPasskeyRepository.CredentialSummary(10L, "admin", "  Windows Hello  ", lastUsedAt, createdAt),
                new AdminPasskeyRepository.CredentialSummary(11L, "admin", "", null, createdAt)
        ));

        var passkeys = service.listCurrentUserPasskeys();

        assertThat(passkeys).hasSize(2);
        assertThat(passkeys.getFirst().id()).isEqualTo(10L);
        assertThat(passkeys.getFirst().displayName()).isEqualTo("Windows Hello");
        assertThat(passkeys.getFirst().lastUsedTime()).isEqualTo(lastUsedAt);
        assertThat(passkeys.get(1).displayName()).isEqualTo("Passkey");
        verify(repository).listByUserId(1L);
    }

    @Test
    void deletesOnlyCurrentUserPasskey() {
        AdminPasskeyRepository repository = mock(AdminPasskeyRepository.class);
        IAuthService authService = mock(IAuthService.class);
        SystemSettingService settingService = mock(SystemSettingService.class);
        PasskeyService service = new PasskeyService(repository, null, authService, null, new ObjectMapper(), settingService);
        when(authService.requireUser()).thenReturn(currentUser());
        when(repository.disableForUser(10L, 1L)).thenReturn(true);

        service.deleteCurrentUserPasskey(10L);

        verify(repository).disableForUser(10L, 1L);
    }

    @Test
    void rejectsMissingPasskeyDelete() {
        AdminPasskeyRepository repository = mock(AdminPasskeyRepository.class);
        IAuthService authService = mock(IAuthService.class);
        SystemSettingService settingService = mock(SystemSettingService.class);
        PasskeyService service = new PasskeyService(repository, null, authService, null, new ObjectMapper(), settingService);
        when(authService.requireUser()).thenReturn(currentUser());
        when(repository.disableForUser(404L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteCurrentUserPasskey(404L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Passkey 不存在或已删除");
    }

    private CurrentUser currentUser() {
        return new CurrentUser(1L, "admin", "管理员", "super_admin", List.of("*"), true);
    }
}
