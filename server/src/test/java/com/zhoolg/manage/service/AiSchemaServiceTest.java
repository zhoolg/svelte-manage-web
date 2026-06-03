package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.AiGenerateRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AiSchemaServiceTest {
    @Test
    void buildsTemplateSchemaWhenModelGatewayHasNoResult() {
        AiSchemaService service = new AiSchemaService(new AiModuleTemplateService(), request -> Optional.empty());
        AiGenerateRequest request = new AiGenerateRequest();
        request.setDescription("生成房源报修管理模块，支持报修、派单、处理和关闭");
        request.setModuleKey("repair_order");
        request.setModuleName("报修管理");
        AiRequirementService.Requirement requirement = new AiRequirementService().normalize(request);

        Map<String, Object> schema = service.buildSchema(requirement, request);

        assertThat(schema)
                .containsEntry("source", "ai-draft")
                .containsEntry("businessType", "crud-workflow")
                .containsEntry("originalPrompt", requirement.description());
        assertThat(schema.get("template").toString()).contains("work-order.repair");
        assertThat(schema.get("entity").toString())
                .contains("RepairOrderEntity")
                .contains("biz_repair_order")
                .contains("reporter")
                .contains("Reporter")
                .contains("Door Window")
                .contains("finishedTime");
        assertThat(schema.get("module").toString()).contains("nameEn=Repair Order");
        assertThat(schema.get("workflow").toString()).contains("assigned", "completed", "closed");
    }

    @Test
    void normalizesModelSchemaAndFallsBackMissingWorkflowToTemplate() {
        AiSchemaService service = new AiSchemaService(
                new AiModuleTemplateService(),
                request -> Optional.of(Map.of(
                        "businessType", "custom-crud",
                        "entity", Map.of(
                                "fields", List.of(
                                        Map.of(
                                                "name", "customer_name",
                                                "label", "客户姓名",
                                                "labelEn", "Customer Name",
                                                "type", "String",
                                                "formType", "input",
                                                "required", true,
                                                "searchable", true
                                        ),
                                        Map.of(
                                                "name", "next_visit_time",
                                                "label", "下次回访时间",
                                                "type", "LocalDateTime",
                                                "formType", "datetime"
                                        )
                                )
                        ),
                        "warnings", List.of(Map.of("message", "模型字段已归一化"))
                ))
        );
        AiGenerateRequest request = new AiGenerateRequest();
        request.setDescription("生成客户回访管理模块，记录客户姓名和下次回访时间");
        request.setModuleKey("customer_revisit");
        request.setModuleName("客户回访");
        AiRequirementService.Requirement requirement = new AiRequirementService().normalize(request);

        Map<String, Object> schema = service.buildSchema(requirement, request);

        assertThat(schema).containsEntry("source", "spring-ai").containsEntry("businessType", "custom-crud");
        assertThat(schema.get("template").toString()).contains("crm.revisit");
        assertThat(schema.get("entity").toString())
                .contains("customerName")
                .contains("Customer Name")
                .contains("Next Visit Time")
                .contains("nextVisitTime")
                .contains("biz_customer_revisit");
        assertThat(schema.get("module").toString()).contains("nameEn=Customer Revisit");
        assertThat(schema.get("workflow").toString()).contains("follow_up", "closed");
        assertThat(schema.get("warnings").toString()).contains("模型字段已归一化");
    }

    @Test
    void passesTransientModelCredentialsToGatewayWithoutPersistingInSchema() {
        AtomicReference<AiModelGateway.GenerationRequest> captured = new AtomicReference<>();
        AiSchemaService service = new AiSchemaService(
                new AiModuleTemplateService(),
                request -> {
                    captured.set(request);
                    return Optional.empty();
                }
        );
        AiGenerateRequest request = new AiGenerateRequest();
        request.setDescription("生成客户回访管理模块");
        request.setModuleKey("customer_revisit");
        request.setModuleName("客户回访");
        request.setProvider("openai");
        request.setModel("gpt-4o-mini");
        request.setApiKey("sk-test");
        request.setBaseUrl("https://api.openai.com");
        AiRequirementService.Requirement requirement = new AiRequirementService().normalize(request);

        Map<String, Object> schema = service.buildSchema(requirement, request);

        assertThat(captured.get().provider()).isEqualTo("openai");
        assertThat(captured.get().model()).isEqualTo("gpt-4o-mini");
        assertThat(captured.get().apiKey()).isEqualTo("sk-test");
        assertThat(captured.get().baseUrl()).isEqualTo("https://api.openai.com");
        assertThat(schema.toString()).doesNotContain("sk-test");
    }
}
