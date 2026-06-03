package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.AiClarificationResponse;
import com.zhoolg.manage.entity.dto.AiGenerateRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiRequirementServiceTest {
    private final AiRequirementService service = new AiRequirementService();

    @Test
    void normalizesRequirementAndInfersRepairDefaults() {
        AiGenerateRequest request = new AiGenerateRequest();
        request.setDescription("  生成房源报修管理模块，支持报修、派单、维修处理、完工和关闭  ");

        AiRequirementService.Requirement requirement = service.normalize(request);

        assertThat(requirement.description()).isEqualTo("生成房源报修管理模块，支持报修、派单、维修处理、完工和关闭");
        assertThat(requirement.moduleKey()).isEqualTo("repair_order");
        assertThat(requirement.moduleName()).isEqualTo("报修管理");
    }

    @Test
    void rejectsInvalidRequirementInput() {
        AiGenerateRequest emptyDescription = new AiGenerateRequest();
        emptyDescription.setDescription(" ");

        assertThatThrownBy(() -> service.normalize(emptyDescription))
                .hasMessageContaining("AI 生成描述不能为空");

        AiGenerateRequest invalidKey = new AiGenerateRequest();
        invalidKey.setDescription("生成报修管理模块");
        invalidKey.setModuleKey("!!!");

        assertThatThrownBy(() -> service.normalize(invalidKey))
                .hasMessageContaining("模块标识不能为空");
    }

    @Test
    void clarifyReportsMissingRequirementDimensions() {
        AiGenerateRequest request = new AiGenerateRequest();
        request.setDescription("生成客户管理");
        request.setModuleKey("customer");
        request.setModuleName("客户管理");
        request.setProvider("openai");

        AiClarificationResponse response = service.clarify(request);

        assertThat(response.needsClarification()).isTrue();
        assertThat(response.qualityScore()).isLessThan(100);
        assertThat(response.requirementsSnapshot())
                .containsEntry("moduleKey", "customer")
                .containsEntry("moduleName", "客户管理")
                .containsEntry("businessType", "crud-workflow");
        assertThat(response.questions())
                .extracting(AiClarificationResponse.Question::id)
                .contains("core_fields", "workflow", "roles_permissions", "page_behavior", "validation_rules");
        assertThat(response.warnings())
                .contains("需求描述偏短，建议补充字段、流程、角色和页面行为。")
                .contains("当前请求声明模型提供商：openai，生成接口会继续兼容该格式。");
    }
}
