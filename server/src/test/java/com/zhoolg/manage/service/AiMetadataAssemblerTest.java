package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.AiValidationReport;
import com.zhoolg.manage.entity.pojo.AiGenerationTaskDO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiMetadataAssemblerTest {
    private final AiMetadataAssembler assembler = new AiMetadataAssembler();

    @Test
    void assemblesCrudMetadataForFrontendRuntime() {
        Map<String, Object> metadata = assembler.toFrontendModuleMetadata(task(), schema());

        assertThat(metadata)
                .containsEntry("id", "repair_order")
                .containsEntry("label", "报修管理")
                .containsEntry("path", "/ai/repair_order")
                .containsEntry("source", "ai-applied")
                .containsEntry("taskNo", "AI-TEST-001");
        assertThat(metadata.get("labelI18n").toString()).contains("zh-CN=报修管理", "en-US=Repair Order");
        assertThat(metadata.get("permissions").toString()).contains("repair_order:view", "repair_order:edit");

        @SuppressWarnings("unchecked")
        Map<String, Object> crud = (Map<String, Object>) metadata.get("crud");
        assertThat(crud)
                .containsEntry("apiBase", "/repair_order")
                .containsEntry("restBase", "/rest/repair_order")
                .containsEntry("showAdd", true)
                .containsEntry("showExport", true);
        assertThat(crud.get("titleI18n").toString()).contains("zh-CN=报修管理", "en-US=Repair Order");
        assertThat(crud.get("columns").toString())
                .contains("reporter")
                .contains("Reporter")
                .contains("statusMap")
                .contains("Pending")
                .contains("datetime");
        assertThat(crud.get("search").toString()).contains("reporter", "status");
        assertThat(crud.get("form").toString())
                .contains("reporter")
                .contains("description")
                .doesNotContain("createTime");
        assertThat(crud.get("workflow").toString())
                .contains("complete")
                .contains("完成")
                .contains("Complete")
                .contains("Confirm Complete?")
                .contains("repair_order:edit")
                .contains("approve")
                .contains("approverRole")
                .contains("timeoutHours")
                .contains("parallelGroup")
                .contains("priority");
        assertThat(crud.get("workflowDefinition").toString())
                .contains("advanced")
                .contains("manager_review")
                .contains("approval")
                .contains("approverRole")
                .contains("parallelGroup")
                .contains("approve");
    }

    @Test
    void previewMetadataReturnsNullWhenValidationFailed() {
        AiValidationReport validation = new AiValidationReport(
                false,
                0,
                List.of(new AiValidationReport.Issue("error", "schema.invalid", "schema invalid"))
        );

        assertThat(assembler.previewMetadata(task(), schema(), validation)).isNull();
    }

    private AiGenerationTaskDO task() {
        AiGenerationTaskDO entity = new AiGenerationTaskDO();
        entity.setTaskNo("AI-TEST-001");
        entity.setModuleKey("repair_order");
        entity.setModuleName("报修管理");
        return entity;
    }

    private Map<String, Object> schema() {
        return Map.of(
                "module", Map.of(
                        "key", "repair_order",
                        "name", "报修管理",
                        "nameEn", "Repair Order",
                        "permissions", List.of("repair_order:view", "repair_order:add", "repair_order:edit")
                ),
                "entity", Map.of(
                        "fields", List.of(
                                Map.of(
                                        "name", "reporter",
                                        "label", "报修人",
                                        "formType", "input",
                                        "required", true,
                                        "table", true,
                                        "searchable", true
                                ),
                                Map.of(
                                        "name", "status",
                                        "label", "状态",
                                        "formType", "select",
                                        "required", true,
                                        "table", true,
                                        "filterable", true,
                                        "options", List.of(
                                                Map.of("label", "待处理", "value", "pending"),
                                                Map.of("label", "已完成", "value", "completed")
                                        )
                                ),
                                Map.of(
                                        "name", "description",
                                        "label", "问题描述",
                                        "formType", "textarea",
                                        "table", false
                                ),
                                Map.of(
                                        "name", "finishedTime",
                                        "label", "完工时间",
                                        "formType", "datetime",
                                        "table", true
                                ),
                                Map.of(
                                        "name", "createTime",
                                        "label", "创建时间",
                                        "formType", "datetime",
                                        "readonly", true
                                )
                        )
                ),
                "workflow", List.of(
                        Map.of("from", "pending", "to", "completed", "action", "complete")
                ),
                "workflowDefinition", Map.of(
                        "mode", "advanced",
                        "nodes", List.of(
                                Map.of("id", "manager_review", "type", "approval", "label", "主管审核", "approverRole", "admin")
                        ),
                        "transitions", List.of(
                                Map.of(
                                        "from", "pending",
                                        "to", "approved",
                                        "action", "approve",
                                        "condition", Map.of("field", "priority", "operator", "gte", "value", 3),
                                        "approverRole", "admin",
                                        "timeoutHours", 24,
                                        "parallelGroup", "review"
                                )
                        )
                )
        );
    }
}
