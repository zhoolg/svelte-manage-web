package com.zhoolg.manage.infrastructure.crud;

import com.zhoolg.manage.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceRegistryTest {
    @Test
    void resolvesStaticResourcesBeforeDynamicResources() {
        DynamicResourceDefinitionProvider provider = mock(DynamicResourceDefinitionProvider.class);
        ResourceRegistry registry = new ResourceRegistry(provider);

        ResourceDefinition definition = registry.require("application");

        assertThat(definition.key()).isEqualTo("applications");
        assertThat(definition.title()).isEqualTo("租房申请");
    }

    @Test
    void exposesMenuManagementAsStaticResource() {
        DynamicResourceDefinitionProvider provider = mock(DynamicResourceDefinitionProvider.class);
        ResourceRegistry registry = new ResourceRegistry(provider);

        ResourceDefinition definition = registry.require("menu");

        assertThat(definition.key()).isEqualTo("menus");
        assertThat(definition.title()).isEqualTo("菜单管理");
        assertThat(definition.permission("view")).isEqualTo("menu:view");
    }

    @Test
    void rejectsUnknownResourcesWhenNoDynamicModuleExists() {
        DynamicResourceDefinitionProvider provider = mock(DynamicResourceDefinitionProvider.class);
        when(provider.find("ghost")).thenReturn(Optional.empty());
        ResourceRegistry registry = new ResourceRegistry(provider);

        assertThatThrownBy(() -> registry.require("ghost"))
                .isInstanceOf(ApiException.class)
                .hasMessage("资源不存在");
    }
}
