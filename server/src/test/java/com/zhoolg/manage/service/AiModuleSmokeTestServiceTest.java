package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.AiSmokeTestResult;
import com.zhoolg.manage.entity.pojo.MenuDO;
import com.zhoolg.manage.entity.pojo.PermissionDO;
import com.zhoolg.manage.infrastructure.crud.DynamicResourceDefinitionProvider;
import com.zhoolg.manage.infrastructure.crud.ResourceDefinition;
import com.zhoolg.manage.mapper.MenuMapper;
import com.zhoolg.manage.mapper.PermissionMapper;
import com.zhoolg.manage.mapper.RolePermissionMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class AiModuleSmokeTestServiceTest {

    @Test
    void runAppliedModuleSmokeTestPassesWhenResourceMenuPermissionsAndRolesAreReady() {
        DynamicResourceDefinitionProvider provider = mock(DynamicResourceDefinitionProvider.class);
        MenuMapper menuMapper = mock(MenuMapper.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        RolePermissionMapper rolePermissionMapper = mock(RolePermissionMapper.class);
        AiRuntimeCrudSmokeTestService runtimeCrudSmokeTestService = mock(AiRuntimeCrudSmokeTestService.class);
        AiModuleSmokeTestService service = new AiModuleSmokeTestService(
                provider,
                menuMapper,
                permissionMapper,
                rolePermissionMapper,
                runtimeCrudSmokeTestService
        );
        ResourceDefinition definition = new ResourceDefinition(
                "repair_order",
                "/rest/repair_order",
                "报修管理",
                "repair_order",
                "id",
                List.of("reporter"),
                List.of("status"),
                List.of("reporter", "status"),
                List.of("reporter", "status"),
                List.of(Map.of("field", "reporter")),
                List.of(Map.of("field", "reporter", "type", "input")),
                List.of(Map.of("field", "reporter", "type", "input"))
        );
        when(provider.find("repair_order")).thenReturn(Optional.of(definition));
        when(runtimeCrudSmokeTestService.run(definition)).thenReturn(List.of(
                new AiSmokeTestResult.Check("runtime_create", "repair_order", "passed", "运行态新增成功"),
                new AiSmokeTestResult.Check("runtime_delete", "repair_order", "passed", "运行态测试数据清理成功")
        ));
        MenuDO menu = new MenuDO();
        menu.setModuleId("repair_order");
        menu.setPermissionCode("repair_order:view");
        menu.setEnabled(true);
        when(menuMapper.selectByModuleId("repair_order")).thenReturn(menu);
        when(permissionMapper.selectEnabled()).thenReturn(List.of(
                permission("repair_order:view"),
                permission("repair_order:add")
        ));
        when(rolePermissionMapper.selectPermissionsByRole("super_admin"))
                .thenReturn(List.of("repair_order:view", "repair_order:add"));
        when(rolePermissionMapper.selectPermissionsByRole("admin"))
                .thenReturn(List.of("repair_order:view", "repair_order:add"));

        AiSmokeTestResult result = service.runAppliedModuleSmokeTest(
                "repair_order",
                Map.of("permissions", List.of("repair_order:view", "repair_order:add"))
        );

        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.checks())
                .extracting(AiSmokeTestResult.Check::status)
                .containsOnly("passed");
        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.repairSuggestions()).isEmpty();
        verify(runtimeCrudSmokeTestService).run(definition);
    }

    @Test
    void runAppliedModuleSmokeTestReturnsDiagnosticsAndRepairSuggestionsWhenFailed() {
        DynamicResourceDefinitionProvider provider = mock(DynamicResourceDefinitionProvider.class);
        MenuMapper menuMapper = mock(MenuMapper.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        RolePermissionMapper rolePermissionMapper = mock(RolePermissionMapper.class);
        AiRuntimeCrudSmokeTestService runtimeCrudSmokeTestService = mock(AiRuntimeCrudSmokeTestService.class);
        AiModuleSmokeTestService service = new AiModuleSmokeTestService(
                provider,
                menuMapper,
                permissionMapper,
                rolePermissionMapper,
                runtimeCrudSmokeTestService
        );
        when(provider.find("repair_order")).thenReturn(Optional.empty());
        when(menuMapper.selectByModuleId("repair_order")).thenReturn(null);
        when(permissionMapper.selectEnabled()).thenReturn(List.of());
        when(rolePermissionMapper.selectPermissionsByRole("super_admin")).thenReturn(List.of());
        when(rolePermissionMapper.selectPermissionsByRole("admin")).thenReturn(List.of());

        AiSmokeTestResult result = service.runAppliedModuleSmokeTest("repair_order", Map.of());

        assertThat(result.passed()).isFalse();
        assertThat(result.score()).isEqualTo(0);
        assertThat(result.diagnostics())
                .extracting(AiSmokeTestResult.Diagnostic::code)
                .contains("resource_definition", "menu", "permission_catalog", "role_binding");
        assertThat(result.diagnostics())
                .extracting(AiSmokeTestResult.Diagnostic::severity)
                .contains("high", "medium");
        assertThat(result.repairSuggestions().toString())
                .contains("重新生成或恢复动态模块元数据")
                .contains("重新执行 AI 模块应用")
                .contains("重新注册动态权限目录")
                .contains("重新绑定管理角色权限");
        verify(runtimeCrudSmokeTestService, never()).run(any());
    }

    private PermissionDO permission(String code) {
        PermissionDO entity = new PermissionDO();
        entity.setPermissionCode(code);
        return entity;
    }
}
