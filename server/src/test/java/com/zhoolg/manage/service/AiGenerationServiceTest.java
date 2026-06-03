package com.zhoolg.manage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.entity.dto.AiApplyPlan;
import com.zhoolg.manage.entity.dto.AiClarificationResponse;
import com.zhoolg.manage.entity.dto.AiGenerateRequest;
import com.zhoolg.manage.entity.dto.AiGenerateResponse;
import com.zhoolg.manage.entity.dto.AiGenerationTaskSummary;
import com.zhoolg.manage.entity.dto.AiPreviewResponse;
import com.zhoolg.manage.entity.dto.AiSmokeTestResult;
import com.zhoolg.manage.entity.dto.AiValidationReport;
import com.zhoolg.manage.entity.pojo.AiGenerationTaskDO;
import com.zhoolg.manage.mapper.AiGenerationTaskMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiGenerationServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatedPreviewUsesBusinessFieldsFlatArchitectureAndMysqlSql() {
        AiGenerationService service = new AiGenerationService(
                mock(AiGenerationTaskMapper.class),
                objectMapper,
                mock(DynamicModuleService.class),
                mock(AiModuleSmokeTestService.class),
                new AiValidationService(),
                new AiGeneratedFileService(objectMapper),
                new AiRequirementService(),
                new AiSchemaService(new AiModuleTemplateService(), request -> Optional.empty()),
                new AiMetadataAssembler()
        );
        AiGenerateRequest request = new AiGenerateRequest();
        request.setDescription("生成房源报修管理模块，支持报修、派单、维修处理、完工和关闭");
        request.setModuleKey("repair_order");
        request.setModuleName("报修管理");

        AiGenerateResponse response = service.generate(request);

        assertThat(response.schema()).containsEntry("businessType", "crud-workflow");
        assertThat(response.schema().get("template").toString()).contains("work-order.repair");
        assertThat(response.schema().get("entity").toString())
                .contains("reporter")
                .contains("property")
                .contains("assignee")
                .contains("finishedTime");
        assertThat(response.schema().get("workflow").toString())
                .contains("assigned")
                .contains("completed")
                .contains("closed");
        assertThat(response.files())
                .extracting(AiGenerateResponse.GeneratedFile::path)
                .contains(
                        "server/src/main/java/com/zhoolg/manage/entity/pojo/RepairOrderDO.java",
                        "server/src/main/java/com/zhoolg/manage/mapper/RepairOrderMapper.java",
                        "server/src/main/java/com/zhoolg/manage/service/RepairOrderService.java",
                        "server/src/main/java/com/zhoolg/manage/service/impl/RepairOrderServiceImpl.java",
                        "server/src/main/java/com/zhoolg/manage/controller/admin/RepairOrderController.java",
                        "server/src/main/resources/generated-sql/create_repair_order.sql"
                )
                .noneMatch(path -> path.contains("com/zhoolg/manage/modules"));
        assertThat(response.files())
                .filteredOn(file -> file.type().equals("sql"))
                .singleElement()
                .satisfies(file -> assertThat(file.content())
                        .contains("reporter VARCHAR(128) NOT NULL")
                        .contains("issue_type VARCHAR(128) NOT NULL")
                        .contains("LONGTEXT")
                        .contains("DATETIME")
                        .contains("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"));
    }

    @Test
    void rejectsOversizedDescription() {
        AiGenerationService service = new AiGenerationService(
                mock(AiGenerationTaskMapper.class),
                objectMapper,
                mock(DynamicModuleService.class),
                mock(AiModuleSmokeTestService.class),
                new AiValidationService(),
                new AiGeneratedFileService(objectMapper),
                new AiRequirementService(),
                new AiSchemaService(new AiModuleTemplateService(), request -> Optional.empty()),
                new AiMetadataAssembler()
        );
        AiGenerateRequest request = new AiGenerateRequest();
        request.setDescription("测".repeat(4001));
        request.setModuleKey("repair_order");
        request.setModuleName("报修管理");

        assertThatThrownBy(() -> service.generate(request))
                .hasMessageContaining("AI 生成描述最长 4000 字符");
    }

    @Test
    void rejectsInvalidModuleKeyAfterNormalization() {
        AiGenerationService service = new AiGenerationService(
                mock(AiGenerationTaskMapper.class),
                objectMapper,
                mock(DynamicModuleService.class),
                mock(AiModuleSmokeTestService.class),
                new AiValidationService(),
                new AiGeneratedFileService(objectMapper),
                new AiRequirementService(),
                new AiSchemaService(new AiModuleTemplateService(), request -> Optional.empty()),
                new AiMetadataAssembler()
        );
        AiGenerateRequest request = new AiGenerateRequest();
        request.setDescription("生成报修管理模块");
        request.setModuleKey("!!!");
        request.setModuleName("报修管理");

        assertThatThrownBy(() -> service.generate(request))
                .hasMessageContaining("模块标识不能为空");
    }

    @Test
    void usesSpringAiSchemaWhenGatewayReturnsStructuredResult() {
        AiGenerationService service = new AiGenerationService(
                mock(AiGenerationTaskMapper.class),
                objectMapper,
                mock(DynamicModuleService.class),
                mock(AiModuleSmokeTestService.class),
                new AiValidationService(),
                new AiGeneratedFileService(objectMapper),
                new AiRequirementService(),
                new AiSchemaService(new AiModuleTemplateService(), request -> Optional.of(java.util.Map.of(
                        "businessType", "crud-workflow",
                        "entity", java.util.Map.of(
                                "fields", java.util.List.of(
                                        java.util.Map.of(
                                                "name", "customer_name",
                                                "label", "客户姓名",
                                                "type", "String",
                                                "formType", "input",
                                                "required", true,
                                                "searchable", true
                                        ),
                                        java.util.Map.of(
                                                "name", "status",
                                                "label", "状态",
                                                "type", "String",
                                                "formType", "select",
                                                "options", java.util.List.of(
                                                        java.util.Map.of("label", "待处理", "value", "pending"),
                                                        java.util.Map.of("label", "已完成", "value", "completed")
                                                )
                                        )
                                )
                        )
                ))),
                new AiMetadataAssembler()
        );
        AiGenerateRequest request = new AiGenerateRequest();
        request.setDescription("生成客户回访管理模块");
        request.setModuleKey("customer_revisit");
        request.setModuleName("客户回访");

        AiGenerateResponse response = service.generate(request);

        assertThat(response.schema()).containsEntry("source", "spring-ai");
        assertThat(response.schema().get("entity").toString())
                .contains("customerName")
                .contains("biz_customer_revisit");
        assertThat(response.files())
                .filteredOn(file -> file.type().equals("sql"))
                .singleElement()
                .satisfies(file -> assertThat(file.content()).contains("customer_name VARCHAR(128) NOT NULL"));
    }

    @Test
    void previewIncludesValidationReport() throws Exception {
        AiGenerationTaskMapper mapper = mock(AiGenerationTaskMapper.class);
        DynamicModuleService dynamicModuleService = mock(DynamicModuleService.class);
        AiGenerationService service = new AiGenerationService(
                mapper,
                objectMapper,
                dynamicModuleService,
                mock(AiModuleSmokeTestService.class),
                new AiValidationService(),
                new AiGeneratedFileService(objectMapper),
                new AiRequirementService(),
                new AiSchemaService(new AiModuleTemplateService(), request -> Optional.empty()),
                new AiMetadataAssembler()
        );
        AiGenerationTaskDO entity = task("AI-TEST-001");
        entity.setGeneratedSchema(objectMapper.writeValueAsString(validSchema()));
        entity.setGeneratedFiles(objectMapper.writeValueAsString(validFiles()));
        when(mapper.selectByTaskNo("AI-TEST-001")).thenReturn(entity);

        AiPreviewResponse response = service.preview("AI-TEST-001");

        assertThat(response.validation().passed()).isTrue();
        assertThat(response.validation().score()).isEqualTo(100);
    }

    @Test
    void clarifyReturnsQuestionsWhenRequirementIsIncomplete() {
        AiGenerationService service = new AiGenerationService(
                mock(AiGenerationTaskMapper.class),
                objectMapper,
                mock(DynamicModuleService.class),
                mock(AiModuleSmokeTestService.class),
                new AiValidationService(),
                new AiGeneratedFileService(objectMapper),
                new AiRequirementService(),
                new AiSchemaService(new AiModuleTemplateService(), request -> Optional.empty()),
                new AiMetadataAssembler()
        );
        AiGenerateRequest request = new AiGenerateRequest();
        request.setDescription("生成客户管理");
        request.setModuleKey("customer");
        request.setModuleName("客户管理");

        AiClarificationResponse response = service.clarify(request);

        assertThat(response.needsClarification()).isTrue();
        assertThat(response.qualityScore()).isLessThan(100);
        assertThat(response.requirementsSnapshot()).containsEntry("moduleKey", "customer");
        assertThat(response.questions())
                .extracting(AiClarificationResponse.Question::id)
                .contains("core_fields", "workflow", "roles_permissions");
    }

    @Test
    void previewReportsQualityIssuesForRiskyGeneratedSchema() throws Exception {
        AiGenerationTaskMapper mapper = mock(AiGenerationTaskMapper.class);
        AiGenerationService service = new AiGenerationService(
                mapper,
                objectMapper,
                mock(DynamicModuleService.class),
                mock(AiModuleSmokeTestService.class),
                new AiValidationService(),
                new AiGeneratedFileService(objectMapper),
                new AiRequirementService(),
                new AiSchemaService(new AiModuleTemplateService(), request -> Optional.empty()),
                new AiMetadataAssembler()
        );
        AiGenerationTaskDO entity = task("AI-TEST-001");
        entity.setGeneratedSchema(objectMapper.writeValueAsString(riskySchema()));
        entity.setGeneratedFiles(objectMapper.writeValueAsString(filesWithoutQueryIndex()));
        when(mapper.selectByTaskNo("AI-TEST-001")).thenReturn(entity);

        AiPreviewResponse response = service.preview("AI-TEST-001");

        assertThat(response.validation().passed()).isFalse();
        assertThat(response.validation().score()).isLessThan(100);
        assertThat(response.validation().issues())
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
    void applyPlanBuildsReviewablePlanBeforeApply() throws Exception {
        AiGenerationTaskMapper mapper = mock(AiGenerationTaskMapper.class);
        DynamicModuleService dynamicModuleService = mock(DynamicModuleService.class);
        AiGenerationService service = new AiGenerationService(
                mapper,
                objectMapper,
                dynamicModuleService,
                mock(AiModuleSmokeTestService.class),
                new AiValidationService(),
                new AiGeneratedFileService(objectMapper),
                new AiRequirementService(),
                new AiSchemaService(new AiModuleTemplateService(), request -> Optional.empty()),
                new AiMetadataAssembler()
        );
        AiGenerationTaskDO entity = task("AI-TEST-001");
        entity.setGeneratedSchema(objectMapper.writeValueAsString(validSchema()));
        entity.setGeneratedFiles(objectMapper.writeValueAsString(validFiles()));
        AiApplyPlan plan = new AiApplyPlan(
                "AI-TEST-001",
                "repair_order",
                "报修管理",
                true,
                88,
                "medium",
                List.of(new AiApplyPlan.Operation("database", "create_table", "biz_repair_order", "创建表", "medium")),
                List.of()
        );
        when(mapper.selectByTaskNo("AI-TEST-001")).thenReturn(entity);
        when(dynamicModuleService.buildApplyPlan(
                eq("AI-TEST-001"),
                eq("repair_order"),
                eq("报修管理"),
                any(),
                any(),
                any(AiValidationReport.class)
        )).thenReturn(plan);

        AiApplyPlan response = service.applyPlan("AI-TEST-001");

        assertThat(response.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.category()).isEqualTo("database");
            assertThat(operation.action()).isEqualTo("create_table");
        });
        verify(dynamicModuleService).buildApplyPlan(
                eq("AI-TEST-001"),
                eq("repair_order"),
                eq("报修管理"),
                any(),
                any(),
                any(AiValidationReport.class)
        );
    }

    @Test
    void rollbackDisablesAppliedDynamicModuleAndUpdatesTaskStatus() {
        AiGenerationTaskMapper mapper = mock(AiGenerationTaskMapper.class);
        DynamicModuleService dynamicModuleService = mock(DynamicModuleService.class);
        AiGenerationService service = new AiGenerationService(
                mapper,
                objectMapper,
                dynamicModuleService,
                mock(AiModuleSmokeTestService.class),
                new AiValidationService(),
                new AiGeneratedFileService(objectMapper),
                new AiRequirementService(),
                new AiSchemaService(new AiModuleTemplateService(), request -> Optional.empty()),
                new AiMetadataAssembler()
        );
        when(mapper.selectByTaskNo("AI-TEST-001")).thenReturn(task("AI-TEST-001"));

        service.rollback("AI-TEST-001");

        verify(dynamicModuleService).rollbackModule("repair_order", "AI-TEST-001");
        verify(mapper).updateStatus("AI-TEST-001", "ROLLED_BACK");
    }

    @Test
    void applyRunsSmokeTestAndStoresResult() throws Exception {
        AiGenerationTaskMapper mapper = mock(AiGenerationTaskMapper.class);
        DynamicModuleService dynamicModuleService = mock(DynamicModuleService.class);
        AiModuleSmokeTestService smokeTestService = mock(AiModuleSmokeTestService.class);
        AiGenerationService service = new AiGenerationService(
                mapper,
                objectMapper,
                dynamicModuleService,
                smokeTestService,
                new AiValidationService(),
                new AiGeneratedFileService(objectMapper),
                new AiRequirementService(),
                new AiSchemaService(new AiModuleTemplateService(), request -> Optional.empty()),
                new AiMetadataAssembler()
        );
        AiGenerationTaskDO entity = task("AI-TEST-001");
        entity.setGeneratedSchema(objectMapper.writeValueAsString(validSchema()));
        entity.setGeneratedFiles(objectMapper.writeValueAsString(validFiles()));
        AiSmokeTestResult smokeTest = new AiSmokeTestResult(
                true,
                100,
                "2026-06-01T18:00:00",
                List.of(new AiSmokeTestResult.Check("resource_definition", "repair_order", "passed", "动态资源定义可加载"))
        );
        when(mapper.selectByTaskNo("AI-TEST-001")).thenReturn(entity);
        when(smokeTestService.runAppliedModuleSmokeTest(eq("repair_order"), any())).thenReturn(smokeTest);

        java.util.Map<String, Object> result = service.apply("AI-TEST-001");

        verify(dynamicModuleService).applyModule(eq("repair_order"), eq("报修管理"), eq("AI-TEST-001"), any(), any());
        verify(smokeTestService).runAppliedModuleSmokeTest(eq("repair_order"), any());
        verify(mapper).updateSmokeTestResult(
                eq("AI-TEST-001"),
                eq("PASSED"),
                org.mockito.ArgumentMatchers.contains("resource_definition")
        );
        verify(mapper).updateStatus("AI-TEST-001", "APPLIED_TO_METADATA");
        assertThat(result).containsEntry("smokeTest", smokeTest);
    }

    @Test
    void applyRetriesOnceWithAutoRepairWhenSmokeTestFails() throws Exception {
        AiGenerationTaskMapper mapper = mock(AiGenerationTaskMapper.class);
        DynamicModuleService dynamicModuleService = mock(DynamicModuleService.class);
        AiModuleSmokeTestService smokeTestService = mock(AiModuleSmokeTestService.class);
        AiAutoRepairService autoRepairService = mock(AiAutoRepairService.class);
        AiMetadataAssembler metadataAssembler = new AiMetadataAssembler();
        AiGenerationService service = new AiGenerationService(
                mapper,
                objectMapper,
                dynamicModuleService,
                smokeTestService,
                new AiValidationService(),
                new AiGeneratedFileService(objectMapper),
                new AiRequirementService(),
                new AiSchemaService(new AiModuleTemplateService(), request -> Optional.empty()),
                metadataAssembler,
                autoRepairService
        );
        AiGenerationTaskDO entity = task("AI-TEST-001");
        entity.setGeneratedSchema(objectMapper.writeValueAsString(validSchema()));
        entity.setGeneratedFiles(objectMapper.writeValueAsString(validFiles()));
        AiSmokeTestResult failedSmokeTest = new AiSmokeTestResult(
                false,
                80,
                "2026-06-02T00:00:00",
                List.of(),
                List.of(new AiSmokeTestResult.Diagnostic(
                        "permission_catalog",
                        "repair_order:view",
                        "high",
                        "权限目录缺失",
                        "重新注册权限"
                )),
                List.of("重新注册权限")
        );
        AiSmokeTestResult passedSmokeTest = new AiSmokeTestResult(
                true,
                100,
                "2026-06-02T00:00:01",
                List.of(new AiSmokeTestResult.Check("permission_catalog", "repair_order:view", "passed", "权限已注册"))
        );
        Map<String, Object> repairedMetadata = new LinkedHashMap<>(metadataAssembler.toFrontendModuleMetadata(entity, validSchema()));
        AiAutoRepairService.RepairAttempt repairAttempt = new AiAutoRepairService.RepairAttempt(
                true,
                validSchema(),
                repairedMetadata,
                List.of("metadata.permissions.defaulted")
        );
        when(mapper.selectByTaskNo("AI-TEST-001")).thenReturn(entity);
        when(smokeTestService.runAppliedModuleSmokeTest(eq("repair_order"), any()))
                .thenReturn(failedSmokeTest, passedSmokeTest);
        when(autoRepairService.repair(eq(entity), any(), any(), eq(failedSmokeTest))).thenReturn(repairAttempt);

        java.util.Map<String, Object> result = service.apply("AI-TEST-001");

        verify(dynamicModuleService, times(2)).applyModule(eq("repair_order"), eq("报修管理"), eq("AI-TEST-001"), any(), any());
        verify(smokeTestService, times(2)).runAppliedModuleSmokeTest(eq("repair_order"), any());
        verify(mapper).updateSmokeTestResult(
                eq("AI-TEST-001"),
                eq("PASSED"),
                org.mockito.ArgumentMatchers.contains("autoRepair")
        );
        assertThat(result.get("autoRepair").toString())
                .contains("REPAIRED")
                .contains("metadata.permissions.defaulted");
    }

    @Test
    void recentTasksReturnsSummaryWithoutGeneratedPayload() {
        AiGenerationTaskMapper mapper = mock(AiGenerationTaskMapper.class);
        AiGenerationService service = new AiGenerationService(
                mapper,
                objectMapper,
                mock(DynamicModuleService.class),
                mock(AiModuleSmokeTestService.class),
                new AiValidationService(),
                new AiGeneratedFileService(objectMapper),
                new AiRequirementService(),
                new AiSchemaService(new AiModuleTemplateService(), request -> Optional.empty()),
                new AiMetadataAssembler()
        );
        AiGenerationTaskDO entity = task("AI-TEST-001");
        entity.setCreateTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        entity.setUpdateTime(LocalDateTime.of(2026, 6, 1, 10, 5));
        when(mapper.selectRecent(20)).thenReturn(List.of(entity));

        List<AiGenerationTaskSummary> tasks = service.recentTasks(20);

        assertThat(tasks).singleElement().satisfies(task -> {
            assertThat(task.taskNo()).isEqualTo("AI-TEST-001");
            assertThat(task.moduleKey()).isEqualTo("repair_order");
            assertThat(task.moduleName()).isEqualTo("报修管理");
            assertThat(task.status()).isEqualTo("APPLIED_TO_METADATA");
            assertThat(task.smokeTestStatus()).isEqualTo("PASSED");
        });
    }

    private AiGenerationTaskDO task(String taskNo) {
        AiGenerationTaskDO entity = new AiGenerationTaskDO();
        entity.setTaskNo(taskNo);
        entity.setModuleKey("repair_order");
        entity.setModuleName("报修管理");
        entity.setStatus("APPLIED_TO_METADATA");
        entity.setSmokeTestStatus("PASSED");
        return entity;
    }

    private java.util.Map<String, Object> validSchema() {
        return java.util.Map.of(
                "module", java.util.Map.of(
                        "key", "repair_order",
                        "permissions", java.util.List.of("repair_order:view")
                ),
                "entity", java.util.Map.of(
                        "tableName", "biz_repair_order",
                        "fields", java.util.List.of(
                                java.util.Map.of("name", "status"),
                                java.util.Map.of("name", "reporter")
                        )
                )
        );
    }

    private java.util.Map<String, Object> riskySchema() {
        return java.util.Map.of(
                "module", java.util.Map.of(
                        "key", "repair_order",
                        "permissions", java.util.List.of("repair_order:view")
                ),
                "entity", java.util.Map.of(
                        "tableName", "biz_repair_order",
                        "fields", java.util.List.of(
                                java.util.Map.of("name", "reporter", "formType", "input", "searchable", true),
                                java.util.Map.of("name", "reporter", "formType", "input", "filterable", true),
                                java.util.Map.of("name", "customer_name", "formType", "input", "searchable", true),
                                java.util.Map.of("name", "idCard", "formType", "input", "searchable", true),
                                java.util.Map.of("name", "bio", "formType", "textarea", "searchable", true),
                                java.util.Map.of(
                                        "name", "status",
                                        "formType", "select",
                                        "filterable", true,
                                        "options", java.util.List.of(
                                                java.util.Map.of("label", "待处理", "value", "pending")
                                        )
                                )
                        )
                ),
                "workflow", java.util.List.of(
                        java.util.Map.of("from", "pending", "to", "done", "action", "complete", "permission", "other:edit")
                )
        );
    }

    private java.util.List<AiGenerateResponse.GeneratedFile> validFiles() {
        return java.util.List.of(
                new AiGenerateResponse.GeneratedFile(
                        "server/src/main/resources/generated-sql/create_repair_order.sql",
                        "sql",
                        "CREATE TABLE biz_repair_order (id BIGINT PRIMARY KEY AUTO_INCREMENT);"
                )
        );
    }

    private java.util.List<AiGenerateResponse.GeneratedFile> filesWithoutQueryIndex() {
        return java.util.List.of(
                new AiGenerateResponse.GeneratedFile(
                        "server/src/main/resources/generated-sql/create_repair_order.sql",
                        "sql",
                        "CREATE TABLE biz_repair_order (id BIGINT, reporter VARCHAR(128));"
                )
        );
    }
}
