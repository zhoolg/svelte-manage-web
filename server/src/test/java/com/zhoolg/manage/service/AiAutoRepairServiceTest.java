package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.AiSmokeTestResult;
import com.zhoolg.manage.entity.pojo.AiGenerationTaskDO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiAutoRepairServiceTest {
    private final AiAutoRepairService service = new AiAutoRepairService(new AiMetadataAssembler());

    @Test
    void repairsMenuAndPermissionMetadataFromSmokeDiagnostics() {
        AiSmokeTestResult failedSmokeTest = new AiSmokeTestResult(
                false,
                40,
                "2026-06-02T00:00:00",
                List.of(),
                List.of(
                        new AiSmokeTestResult.Diagnostic("menu", "repair_order", "medium", "菜单未启用", "修复菜单"),
                        new AiSmokeTestResult.Diagnostic("permission_catalog", "repair_order:view", "high", "权限缺失", "修复权限")
                ),
                List.of("修复菜单", "修复权限")
        );

        AiAutoRepairService.RepairAttempt attempt = service.repair(
                task(),
                schemaWithoutPermissions(),
                Map.of("crud", Map.of("columns", List.of(), "form", List.of())),
                failedSmokeTest
        );

        assertThat(attempt.changed()).isTrue();
        assertThat(attempt.actions())
                .contains(
                        "metadata.permissions.defaulted",
                        "schema.module.permissions.defaulted",
                        "metadata.id.defaulted",
                        "metadata.label.defaulted",
                        "metadata.icon.defaulted",
                        "metadata.path.defaulted"
                );
        assertThat(attempt.metadata().get("permissions").toString())
                .contains("repair_order:view", "repair_order:delete", "repair_order:export");
        assertThat(attempt.metadata())
                .containsEntry("id", "repair_order")
                .containsEntry("path", "/ai/repair_order");
        assertThat(attempt.metadata().get("autoRepair").toString()).contains("ATTEMPTED");
        assertThat(attempt.schema().get("module").toString()).contains("repair_order:view");
    }

    private AiGenerationTaskDO task() {
        AiGenerationTaskDO entity = new AiGenerationTaskDO();
        entity.setTaskNo("AI-TEST-001");
        entity.setModuleKey("repair_order");
        entity.setModuleName("报修管理");
        return entity;
    }

    private Map<String, Object> schemaWithoutPermissions() {
        return Map.of(
                "module", Map.of("key", "repair_order", "name", "报修管理"),
                "entity", Map.of(
                        "fields", List.of(
                                Map.of("name", "title", "label", "标题", "formType", "input", "table", true),
                                Map.of("name", "status", "label", "状态", "formType", "select", "table", true)
                        )
                )
        );
    }
}
