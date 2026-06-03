package com.zhoolg.manage.service.impl;

import com.zhoolg.manage.entity.base.CurrentUser;
import com.zhoolg.manage.mapper.PermissionMapper;
import com.zhoolg.manage.mapper.RolePermissionMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionServiceImplTest {

    @Test
    void usesDatabasePermissionsForRole() {
        RolePermissionMapper mapper = mock(RolePermissionMapper.class);
        when(mapper.selectPermissionsByRole("operator")).thenReturn(List.of("dashboard:view", "settings:*"));
        PermissionServiceImpl service = new PermissionServiceImpl(mapper, mock(PermissionMapper.class));

        assertThat(service.permissionsForRole("operator")).containsExactly("dashboard:view", "settings:*");
    }

    @Test
    void fallsBackToBuiltInPermissionsWhenRoleHasNoDatabaseRows() {
        RolePermissionMapper mapper = mock(RolePermissionMapper.class);
        when(mapper.selectPermissionsByRole("viewer")).thenReturn(List.of());
        PermissionServiceImpl service = new PermissionServiceImpl(mapper, mock(PermissionMapper.class));

        assertThat(service.permissionsForRole("viewer")).contains("dashboard:view", "settings:view");
    }

    @Test
    void keepsWildcardPermissionMatching() {
        RolePermissionMapper mapper = mock(RolePermissionMapper.class);
        PermissionServiceImpl service = new PermissionServiceImpl(mapper, mock(PermissionMapper.class));
        CurrentUser user = new CurrentUser(1L, "operator", "运营人员", "operator", List.of("settings:*"), false);

        assertThat(service.hasPermission(user, "settings:edit")).isTrue();
        assertThat(service.hasPermission(user, "admin:delete")).isFalse();
    }
}
