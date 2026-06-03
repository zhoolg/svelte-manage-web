package com.zhoolg.manage.controller.admin;

import com.zhoolg.manage.entity.pojo.MenuDO;
import com.zhoolg.manage.infrastructure.crud.ResourceRegistry;
import com.zhoolg.manage.mapper.MenuMapper;
import com.zhoolg.manage.service.DynamicModuleService;
import com.zhoolg.manage.service.IAuthService;
import com.zhoolg.manage.service.IPermissionService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetadataControllerTest {

    @Test
    void menuCompletesMissingBuiltinItemsWhenDatabaseMenuIsPartial() {
        MenuMapper menuMapper = mock(MenuMapper.class);
        DynamicModuleService dynamicModuleService = mock(DynamicModuleService.class);
        MetadataController controller = new MetadataController(
                mock(ResourceRegistry.class),
                dynamicModuleService,
                mock(IAuthService.class),
                mock(IPermissionService.class),
                menuMapper
        );
        when(dynamicModuleService.enabledModules()).thenReturn(List.of());
        when(menuMapper.selectEnabled()).thenReturn(List.of(
                menu("system", null, "menu.system", "cog", "/system", null, null),
                menu("settings", "system", "menu.settings", "settings", "/settings", "settings", "settings:view")
        ));

        List<Map<String, Object>> menus = controller.menu().data();
        List<Map<String, Object>> flatMenus = flatten(menus);

        assertThat(flatMenus)
                .extracting(item -> item.get("id"))
                .contains("home", "applications", "faq", "admins", "logs", "dict", "settings", "menus", "ai-modules");
        assertThat(flatMenus)
                .extracting(item -> item.get("id"))
                .doesNotContain("system-status");
    }

    private MenuDO menu(
            String key,
            String parentKey,
            String label,
            String icon,
            String path,
            String moduleId,
            String permission
    ) {
        MenuDO menu = new MenuDO();
        menu.setMenuKey(key);
        menu.setParentKey(parentKey);
        menu.setLabel(label);
        menu.setIcon(icon);
        menu.setPath(path);
        menu.setModuleId(moduleId);
        menu.setPermissionCode(permission);
        menu.setEnabled(true);
        return menu;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> flatten(List<Map<String, Object>> nodes) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> node : nodes) {
            result.add(node);
            Object children = node.get("children");
            if (children instanceof List<?> list) {
                result.addAll(flatten((List<Map<String, Object>>) list));
            }
        }
        return result;
    }
}
