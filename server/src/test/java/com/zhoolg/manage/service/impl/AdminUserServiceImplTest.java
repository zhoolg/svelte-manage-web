package com.zhoolg.manage.service.impl;

import com.zhoolg.manage.entity.dto.UpdateAdminDTO;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.mapper.struct.AdminUserStructMapper;
import com.zhoolg.manage.service.UserDirectoryService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminUserServiceImplTest {
    private final AdminUserStructMapper structMapper = new AdminUserStructMapper();

    @Test
    void rejectsDeletingLastActiveSuperAdmin() {
        UserDirectoryService userDirectory = mock(UserDirectoryService.class);
        AdminUserServiceImpl service = new AdminUserServiceImpl(userDirectory, mock(PasswordEncoder.class), structMapper);
        UserDirectoryService.Account account = account(1L, "super_admin", true);
        when(userDirectory.findById(1L)).thenReturn(Optional.of(account));
        when(userDirectory.findAll()).thenReturn(List.of(account));

        assertThatThrownBy(() -> service.deleteAdmin(1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("至少保留一个启用的超级管理员");
    }

    @Test
    void rejectsDisablingLastActiveSuperAdminByUpdate() {
        UserDirectoryService userDirectory = mock(UserDirectoryService.class);
        AdminUserServiceImpl service = new AdminUserServiceImpl(userDirectory, mock(PasswordEncoder.class), structMapper);
        UserDirectoryService.Account account = account(1L, "super_admin", true);
        when(userDirectory.findById(1L)).thenReturn(Optional.of(account));
        when(userDirectory.findAll()).thenReturn(List.of(account));

        assertThatThrownBy(() -> service.updateAdmin(1L, new UpdateAdminDTO(null, null, false)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("至少保留一个启用的超级管理员");
    }

    private UserDirectoryService.Account account(Long id, String roleCode, boolean enabled) {
        return new UserDirectoryService.Account(id, "admin" + id, "hash", "管理员" + id, null, roleCode, enabled, null, null);
    }
}
