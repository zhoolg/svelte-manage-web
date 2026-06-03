package com.zhoolg.manage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.entity.dto.AiApplyPlan;
import com.zhoolg.manage.entity.dto.AiValidationReport;
import com.zhoolg.manage.entity.pojo.DynamicModuleDO;
import com.zhoolg.manage.entity.pojo.DynamicModuleVersionDO;
import com.zhoolg.manage.entity.pojo.MenuDO;
import com.zhoolg.manage.infrastructure.crud.DynamicTableService;
import com.zhoolg.manage.mapper.DynamicModuleMapper;
import com.zhoolg.manage.mapper.DynamicModuleVersionMapper;
import com.zhoolg.manage.mapper.MenuMapper;
import com.zhoolg.manage.mapper.PermissionMapper;
import com.zhoolg.manage.mapper.RolePermissionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicModuleServiceTest {

    @Test
    void applyModuleCreatesDynamicTableAndStoresMetadata() {
        DynamicModuleMapper mapper = mock(DynamicModuleMapper.class);
        DynamicModuleVersionMapper versionMapper = mock(DynamicModuleVersionMapper.class);
        MenuMapper menuMapper = mock(MenuMapper.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        RolePermissionMapper rolePermissionMapper = mock(RolePermissionMapper.class);
        DynamicTableService dynamicTableService = mock(DynamicTableService.class);
        DynamicModuleService service = new DynamicModuleService(
                mapper,
                versionMapper,
                menuMapper,
                permissionMapper,
                rolePermissionMapper,
                new ObjectMapper(),
                dynamicTableService
        );
        Map<String, Object> metadata = metadata("报修管理");
        Map<String, Object> schema = schema();
        when(versionMapper.nextVersionNo("repair_order")).thenReturn(1);

        service.applyModule("repair_order", "报修管理", "AI-TEST-001", metadata, schema);

        verify(dynamicTableService).ensureTable("repair_order", schema);
        ArgumentCaptor<DynamicModuleVersionDO> versionCaptor = ArgumentCaptor.forClass(DynamicModuleVersionDO.class);
        verify(versionMapper).insert(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getVersionNo()).isEqualTo(1);
        assertThat(versionCaptor.getValue().getSchemaJson()).contains("reporter");
        ArgumentCaptor<DynamicModuleDO> entityCaptor = ArgumentCaptor.forClass(DynamicModuleDO.class);
        verify(mapper).upsert(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getModuleKey()).isEqualTo("repair_order");
        assertThat(entityCaptor.getValue().getCurrentVersionNo()).isEqualTo(1);
        assertThat(entityCaptor.getValue().getSchemaHash()).hasSize(64);
        assertThat(entityCaptor.getValue().getMetadataJson()).contains("报修管理");
        verify(permissionMapper).upsert(org.mockito.ArgumentMatchers.argThat(permission ->
                "repair_order:view".equals(permission.getPermissionCode())
        ));
        verify(rolePermissionMapper).upsertRolePermission("super_admin", "repair_order:view");
        verify(rolePermissionMapper).upsertRolePermission("admin", "repair_order:view");
        verify(menuMapper).insert(org.mockito.ArgumentMatchers.any(MenuDO.class));
    }

    @Test
    void applyModuleRefreshesExistingMenuMetadata() {
        DynamicModuleMapper mapper = mock(DynamicModuleMapper.class);
        DynamicModuleVersionMapper versionMapper = mock(DynamicModuleVersionMapper.class);
        MenuMapper menuMapper = mock(MenuMapper.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        RolePermissionMapper rolePermissionMapper = mock(RolePermissionMapper.class);
        DynamicTableService dynamicTableService = mock(DynamicTableService.class);
        DynamicModuleService service = new DynamicModuleService(
                mapper,
                versionMapper,
                menuMapper,
                permissionMapper,
                rolePermissionMapper,
                new ObjectMapper(),
                dynamicTableService
        );
        MenuDO existing = new MenuDO();
        existing.setId(12L);
        existing.setMenuKey("ai-repair_order");
        existing.setParentKey("system");
        existing.setLabel("旧名称");
        existing.setIcon("circle");
        existing.setPath("/ai/old");
        existing.setModuleId("repair_order");
        existing.setPermissionCode("repair_order:view");
        existing.setSortOrder(1000);
        existing.setHidden(false);
        existing.setEnabled(false);
        existing.setSystemBuiltin(false);
        when(menuMapper.selectByModuleId("repair_order")).thenReturn(existing);
        when(versionMapper.nextVersionNo("repair_order")).thenReturn(2);

        service.applyModule("repair_order", "报修管理", "AI-TEST-001", metadata("新报修"), schema());

        verify(menuMapper).update(existing);
        assertThat(existing.getLabel()).isEqualTo("新报修");
        assertThat(existing.getIcon()).isEqualTo("sparkles");
        assertThat(existing.getPath()).isEqualTo("/ai/repair_order");
        assertThat(existing.getEnabled()).isTrue();
    }

    @Test
    void buildApplyPlanShowsDatabaseMenuMetadataAndPermissionOperations() {
        DynamicModuleMapper mapper = mock(DynamicModuleMapper.class);
        DynamicModuleVersionMapper versionMapper = mock(DynamicModuleVersionMapper.class);
        MenuMapper menuMapper = mock(MenuMapper.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        RolePermissionMapper rolePermissionMapper = mock(RolePermissionMapper.class);
        DynamicTableService dynamicTableService = mock(DynamicTableService.class);
        DynamicModuleService service = new DynamicModuleService(
                mapper,
                versionMapper,
                menuMapper,
                permissionMapper,
                rolePermissionMapper,
                new ObjectMapper(),
                dynamicTableService
        );
        when(dynamicTableService.tablePlan("repair_order", schema())).thenReturn(List.of(
                new AiApplyPlan.Operation("database", "create_table", "biz_repair_order", "创建动态表", "medium")
        ));
        when(versionMapper.nextVersionNo("repair_order")).thenReturn(1);

        AiApplyPlan plan = service.buildApplyPlan(
                "AI-TEST-001",
                "repair_order",
                "报修管理",
                metadata("报修管理"),
                schema(),
                new AiValidationReport(true, 100, List.of())
        );

        assertThat(plan.canApply()).isTrue();
        assertThat(plan.riskLevel()).isEqualTo("medium");
        assertThat(plan.operations())
                .extracting(AiApplyPlan.Operation::action)
                .contains("create_table", "create_module", "create_version", "create_menu", "expose_crud", "register_permission", "bind_admin_roles");
        assertThat(plan.warnings()).isNotEmpty();
    }

    @Test
    void restoreVersionRebuildsTableAndPointsCurrentModuleToSelectedVersion() throws Exception {
        DynamicModuleMapper mapper = mock(DynamicModuleMapper.class);
        DynamicModuleVersionMapper versionMapper = mock(DynamicModuleVersionMapper.class);
        MenuMapper menuMapper = mock(MenuMapper.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        RolePermissionMapper rolePermissionMapper = mock(RolePermissionMapper.class);
        DynamicTableService dynamicTableService = mock(DynamicTableService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        DynamicModuleService service = new DynamicModuleService(
                mapper,
                versionMapper,
                menuMapper,
                permissionMapper,
                rolePermissionMapper,
                objectMapper,
                dynamicTableService
        );
        DynamicModuleVersionDO version = new DynamicModuleVersionDO();
        version.setModuleKey("repair_order");
        version.setModuleName("报修管理");
        version.setVersionNo(2);
        version.setTaskNo("AI-TEST-002");
        version.setSchemaHash("a".repeat(64));
        version.setMetadataJson(objectMapper.writeValueAsString(metadata("报修管理")));
        version.setSchemaJson(objectMapper.writeValueAsString(schema()));
        when(versionMapper.selectByModuleKeyAndVersionNo("repair_order", 2)).thenReturn(version);

        Map<String, Object> result = service.restoreVersion("repair_order", 2);

        verify(dynamicTableService).ensureTable(org.mockito.ArgumentMatchers.eq("repair_order"), org.mockito.ArgumentMatchers.anyMap());
        ArgumentCaptor<DynamicModuleDO> entityCaptor = ArgumentCaptor.forClass(DynamicModuleDO.class);
        verify(mapper).upsert(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getCurrentVersionNo()).isEqualTo(2);
        assertThat(entityCaptor.getValue().getTaskNo()).isEqualTo("AI-TEST-002");
        verify(permissionMapper).upsert(org.mockito.ArgumentMatchers.argThat(permission ->
                "repair_order:view".equals(permission.getPermissionCode())
        ));
        verify(rolePermissionMapper).upsertRolePermission("super_admin", "repair_order:view");
        verify(rolePermissionMapper).upsertRolePermission("admin", "repair_order:view");
        assertThat(result.get("versionNo")).isEqualTo(2);
    }

    @Test
    void saveDesignerMetadataCreatesNewVersionWithCurrentSchema() throws Exception {
        DynamicModuleMapper mapper = mock(DynamicModuleMapper.class);
        DynamicModuleVersionMapper versionMapper = mock(DynamicModuleVersionMapper.class);
        MenuMapper menuMapper = mock(MenuMapper.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        RolePermissionMapper rolePermissionMapper = mock(RolePermissionMapper.class);
        DynamicTableService dynamicTableService = mock(DynamicTableService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        DynamicModuleService service = new DynamicModuleService(
                mapper,
                versionMapper,
                menuMapper,
                permissionMapper,
                rolePermissionMapper,
                objectMapper,
                dynamicTableService
        );
        DynamicModuleDO current = new DynamicModuleDO();
        current.setModuleKey("repair_order");
        current.setModuleName("报修管理");
        current.setTaskNo("AI-TEST-002");
        current.setCurrentVersionNo(2);
        current.setMetadataJson(objectMapper.writeValueAsString(metadata("报修管理")));
        current.setEnabled(true);
        DynamicModuleVersionDO version = new DynamicModuleVersionDO();
        version.setModuleKey("repair_order");
        version.setModuleName("报修管理");
        version.setVersionNo(2);
        version.setTaskNo("AI-TEST-002");
        version.setSchemaHash("b".repeat(64));
        version.setMetadataJson(objectMapper.writeValueAsString(metadata("报修管理")));
        version.setSchemaJson(objectMapper.writeValueAsString(schema()));
        when(mapper.selectByModuleKey("repair_order")).thenReturn(current);
        when(versionMapper.selectByModuleKeyAndVersionNo("repair_order", 2)).thenReturn(version);
        when(versionMapper.nextVersionNo("repair_order")).thenReturn(3);

        Map<String, Object> result = service.saveDesignerMetadata("repair_order", designerMetadata("设计器报修"));

        verify(dynamicTableService).ensureTable(org.mockito.ArgumentMatchers.eq("repair_order"), org.mockito.ArgumentMatchers.anyMap());
        ArgumentCaptor<DynamicModuleVersionDO> versionCaptor = ArgumentCaptor.forClass(DynamicModuleVersionDO.class);
        verify(versionMapper).insert(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getVersionNo()).isEqualTo(3);
        assertThat(versionCaptor.getValue().getTaskNo()).isEqualTo("AI-TEST-002");
        assertThat(versionCaptor.getValue().getMetadataJson()).contains("设计器报修");
        ArgumentCaptor<DynamicModuleDO> moduleCaptor = ArgumentCaptor.forClass(DynamicModuleDO.class);
        verify(mapper).upsert(moduleCaptor.capture());
        assertThat(moduleCaptor.getValue().getCurrentVersionNo()).isEqualTo(3);
        assertThat(result.get("taskNo").toString()).isEqualTo("AI-TEST-002");
    }

    private Map<String, Object> metadata(String label) {
        return Map.of(
                "id", "repair_order",
                "label", label,
                "icon", "sparkles",
                "path", "/ai/repair_order",
                "permissions", List.of("repair_order:view", "repair_order:add"),
                "crud", Map.of("title", label)
        );
    }

    private Map<String, Object> schema() {
        return Map.of(
                "entity", Map.of(
                        "fields", List.of(
                                Map.of("name", "reporter", "type", "String", "formType", "input", "required", true),
                                Map.of("name", "status", "type", "String", "formType", "select", "required", true)
                        )
                )
        );
    }

    private Map<String, Object> designerMetadata(String label) {
        return Map.of(
                "id", "repair_order",
                "label", label,
                "icon", "sparkles",
                "path", "/ai/repair_order",
                "permissions", List.of("repair_order:view", "repair_order:add"),
                "crud", Map.of(
                        "title", label,
                        "apiBase", "/repair_order",
                        "columns", List.of(Map.of("field", "reporter", "label", "报修人")),
                        "search", List.of(Map.of("field", "reporter", "label", "报修人", "type", "input")),
                        "form", List.of(Map.of("field", "reporter", "label", "报修人", "type", "input", "required", true))
                )
        );
    }
}
