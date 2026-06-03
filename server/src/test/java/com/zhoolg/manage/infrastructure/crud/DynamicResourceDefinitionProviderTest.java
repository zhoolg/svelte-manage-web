package com.zhoolg.manage.infrastructure.crud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.entity.pojo.DynamicModuleDO;
import com.zhoolg.manage.mapper.DynamicModuleMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicResourceDefinitionProviderTest {
    private final DynamicModuleMapper mapper = mock(DynamicModuleMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DynamicResourceDefinitionProvider provider = new DynamicResourceDefinitionProvider(mapper, objectMapper);

    @Test
    void buildsResourceDefinitionFromDynamicModuleMetadata() throws Exception {
        DynamicModuleDO entity = new DynamicModuleDO();
        entity.setModuleKey("repair_order");
        entity.setModuleName("报修管理");
        entity.setEnabled(true);
        entity.setMetadataJson(objectMapper.writeValueAsString(Map.of(
                "id", "repair_order",
                "label", "报修管理",
                "path", "/ai/repair_order",
                "permissions", List.of("repair_order:view", "repair_order:add", "repair_order:edit"),
                "crud", Map.of(
                        "title", "报修管理",
                        "restBase", "/rest/repair_order",
                        "columns", List.of(
                                Map.of("field", "reporter", "label", "报修人"),
                                Map.of("field", "status", "label", "状态", "format", "status")
                        ),
                        "search", List.of(
                                Map.of("field", "reporter", "label", "报修人", "type", "input"),
                                Map.of("field", "status", "label", "状态", "type", "select")
                        ),
                        "form", List.of(
                                Map.of("field", "reporter", "label", "报修人", "type", "input", "required", true),
                                Map.of("field", "status", "label", "状态", "type", "select", "required", true),
                                Map.of("field", "createTime", "label", "创建时间", "type", "datetime", "disabled", true)
                        )
                )
        )));
        when(mapper.selectByModuleKey("repair_order")).thenReturn(entity);

        ResourceDefinition definition = provider.find("repair_order").orElseThrow();

        assertThat(definition.key()).isEqualTo("repair_order");
        assertThat(definition.title()).isEqualTo("报修管理");
        assertThat(definition.permission("view")).isEqualTo("repair_order:view");
        assertThat(definition.allowedCreateFields()).containsExactly("reporter", "status");
        assertThat(definition.allowedUpdateFields()).containsExactly("reporter", "status");
        assertThat(definition.searchableFields()).containsExactly("reporter");
        assertThat(definition.filterFields()).containsExactly("status");
        assertThat(definition.columns()).hasSize(2);
        assertThat(definition.form()).hasSize(3);
    }

    @Test
    void ignoresDisabledDynamicModule() {
        DynamicModuleDO entity = new DynamicModuleDO();
        entity.setModuleKey("repair_order");
        entity.setEnabled(false);
        when(mapper.selectByModuleKey("repair_order")).thenReturn(entity);

        assertThat(provider.find("repair_order")).isEmpty();
        assertThat(provider.exists("repair_order")).isFalse();
    }
}
