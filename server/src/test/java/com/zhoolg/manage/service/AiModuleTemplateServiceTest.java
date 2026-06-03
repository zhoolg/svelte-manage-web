package com.zhoolg.manage.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiModuleTemplateServiceTest {

    @Test
    void selectTemplateReturnsRepairTemplateWithWorkflow() {
        AiModuleTemplateService service = new AiModuleTemplateService();

        AiModuleTemplateService.TemplateDefinition template = service.selectTemplate(
                "repair_order",
                "生成房源报修管理模块"
        );

        assertThat(template.key()).isEqualTo("work-order.repair");
        assertThat(template.fields())
                .extracting(field -> field.get("name"))
                .contains("reporter", "issueType", "status", "finishedTime");
        assertThat(template.workflow())
                .extracting(step -> step.get("action"))
                .contains("assign", "start", "complete", "close");
    }

    @Test
    void selectTemplateFallsBackToGenericCrudTemplate() {
        AiModuleTemplateService service = new AiModuleTemplateService();

        AiModuleTemplateService.TemplateDefinition template = service.selectTemplate(
                "custom_module",
                "生成一个自定义管理模块"
        );

        assertThat(template.key()).isEqualTo("generic.crud");
        assertThat(template.fields())
                .extracting(field -> field.get("name"))
                .contains("title", "description", "status");
    }
}
