package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.AiGenerateResponse;
import com.zhoolg.manage.entity.dto.AiValidationReport;
import com.zhoolg.manage.entity.pojo.AiGenerationTaskDO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiValidationServiceTest {
    private final AiValidationService service = new AiValidationService();

    @Test
    void acceptsValidFlatMysqlGeneratedModule() {
        AiValidationReport report = service.validate(
                task(),
                validSchema(),
                List.of(new AiGenerateResponse.GeneratedFile(
                        "server/src/main/resources/generated-sql/create_repair_order.sql",
                        "sql",
                        "CREATE TABLE biz_repair_order (id BIGINT PRIMARY KEY AUTO_INCREMENT);"
                ))
        );

        assertThat(report.passed()).isTrue();
        assertThat(report.score()).isEqualTo(100);
        assertThat(report.issues()).isEmpty();
    }

    @Test
    void rejectsLegacyPackageAndMissingCoreSchema() {
        AiValidationReport report = service.validate(
                task(),
                Map.of(),
                List.of(new AiGenerateResponse.GeneratedFile(
                        "server/src/main/java/com/zhoolg/manage/modules/RepairOrder.java",
                        "java",
                        "package com.zhoolg.manage.modules;"
                ))
        );

        assertThat(report.passed()).isFalse();
        assertThat(report.issues())
                .extracting(AiValidationReport.Issue::code)
                .contains("schema.module.missing", "schema.entity.missing", "files.legacy.path", "files.sql.missing");
    }

    @Test
    void reportsWorkflowAndFieldQualityProblems() {
        AiValidationReport report = service.validate(
                task(),
                riskySchema(),
                List.of(new AiGenerateResponse.GeneratedFile(
                        "server/src/main/resources/generated-sql/create_repair_order.sql",
                        "sql",
                        "CREATE TABLE biz_repair_order (id BIGINT, reporter VARCHAR(128));"
                ))
        );

        assertThat(report.passed()).isFalse();
        assertThat(report.issues())
                .extracting(AiValidationReport.Issue::code)
                .contains(
                        "entity.field.name.duplicate",
                        "entity.field.name.style",
                        "entity.search.long_text",
                        "entity.field.sensitive",
                        "workflow.status.mismatch",
                        "workflow.permission.invalid",
                        "entity.index.suggestion"
                );
    }

    @Test
    void validatesAdvancedWorkflowConditionAndTimeoutMetadata() {
        AiValidationReport report = service.validate(
                task(),
                advancedWorkflowSchema(),
                List.of(new AiGenerateResponse.GeneratedFile(
                        "server/src/main/resources/generated-sql/create_repair_order.sql",
                        "sql",
                        "CREATE TABLE biz_repair_order (id BIGINT, status VARCHAR(32), priority INT, INDEX idx_status(status));"
                ))
        );

        assertThat(report.passed()).isFalse();
        assertThat(report.issues())
                .extracting(AiValidationReport.Issue::code)
                .contains(
                        "workflow.approver.missing",
                        "workflow.timeout.invalid",
                        "workflow.condition.field_missing",
                        "workflow.condition.operator_invalid"
                );
    }

    private AiGenerationTaskDO task() {
        AiGenerationTaskDO entity = new AiGenerationTaskDO();
        entity.setModuleKey("repair_order");
        entity.setModuleName("报修管理");
        entity.setTaskNo("AI-TEST-001");
        return entity;
    }

    private Map<String, Object> validSchema() {
        return Map.of(
                "module", Map.of(
                        "key", "repair_order",
                        "permissions", List.of("repair_order:view")
                ),
                "entity", Map.of(
                        "tableName", "biz_repair_order",
                        "fields", List.of(
                                Map.of("name", "status"),
                                Map.of("name", "reporter")
                        )
                )
        );
    }

    private Map<String, Object> riskySchema() {
        return Map.of(
                "module", Map.of(
                        "key", "repair_order",
                        "permissions", List.of("repair_order:view")
                ),
                "entity", Map.of(
                        "tableName", "biz_repair_order",
                        "fields", List.of(
                                Map.of("name", "reporter", "formType", "input", "searchable", true),
                                Map.of("name", "reporter", "formType", "input", "filterable", true),
                                Map.of("name", "customer_name", "formType", "input", "searchable", true),
                                Map.of("name", "idCard", "formType", "input", "searchable", true),
                                Map.of("name", "bio", "formType", "textarea", "searchable", true),
                                Map.of(
                                        "name", "status",
                                        "formType", "select",
                                        "filterable", true,
                                        "options", List.of(Map.of("label", "待处理", "value", "pending"))
                                )
                        )
                ),
                "workflow", List.of(
                        Map.of("from", "pending", "to", "done", "action", "complete", "permission", "other:edit")
                )
        );
    }

    private Map<String, Object> advancedWorkflowSchema() {
        return Map.of(
                "module", Map.of(
                        "key", "repair_order",
                        "permissions", List.of("repair_order:view", "repair_order:edit")
                ),
                "entity", Map.of(
                        "tableName", "biz_repair_order",
                        "fields", List.of(
                                Map.of(
                                        "name", "status",
                                        "formType", "select",
                                        "options", List.of(
                                                Map.of("label", "待提交", "value", "submitted"),
                                                Map.of("label", "已审批", "value", "approved")
                                        )
                                ),
                                Map.of("name", "priority", "formType", "input")
                        )
                ),
                "workflowDefinition", Map.of(
                        "mode", "advanced",
                        "nodes", List.of(
                                Map.of("id", "manager_review", "type", "approval", "timeoutHours", -1)
                        ),
                        "transitions", List.of(
                                Map.of(
                                        "from", "submitted",
                                        "to", "approved",
                                        "action", "approve",
                                        "permission", "repair_order:edit",
                                        "timeoutHours", 0,
                                        "condition", Map.of("field", "missingField", "operator", "between", "value", 3)
                                )
                        )
                )
        );
    }
}
