package com.zhoolg.manage.infrastructure.crud;

import com.zhoolg.manage.entity.base.PageResult;
import com.zhoolg.manage.entity.dto.AdminUserDTO;
import com.zhoolg.manage.entity.pojo.ApplicationDO;
import com.zhoolg.manage.entity.pojo.MenuDO;
import com.zhoolg.manage.entity.pojo.SystemSettingDO;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.mapper.ApplicationMapper;
import com.zhoolg.manage.mapper.AuditLogMapper;
import com.zhoolg.manage.mapper.DictMapper;
import com.zhoolg.manage.mapper.FaqMapper;
import com.zhoolg.manage.mapper.MenuMapper;
import com.zhoolg.manage.mapper.SystemSettingMapper;
import com.zhoolg.manage.mapper.struct.ResourceStructMapper;
import com.zhoolg.manage.service.IAdminUserService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseResourceRepositoryTest {

    @Test
    void readsSettingsFromStructuredSettingTable() {
        SystemSettingMapper systemSettingMapper = mock(SystemSettingMapper.class);
        DatabaseResourceRepository repository = repository(systemSettingMapper);
        SystemSettingDO setting = setting(10L, "site.title", "站点标题", "管理平台", true);
        when(systemSettingMapper.selectAll()).thenReturn(List.of(setting));

        List<Map<String, Object>> rows = repository.findAll("settings");

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("id", 10L)
                .containsEntry("key", "site.title")
                .containsEntry("name", "站点标题")
                .containsEntry("value", "管理平台")
                .containsEntry("systemBuiltin", true);
    }

    @Test
    void rejectsDeletingBuiltInSettings() {
        SystemSettingMapper systemSettingMapper = mock(SystemSettingMapper.class);
        DatabaseResourceRepository repository = repository(systemSettingMapper);
        when(systemSettingMapper.selectById(10L)).thenReturn(setting(10L, "site.title", "站点标题", "管理平台", true));

        assertThatThrownBy(() -> repository.delete(resource("settings"), 10L))
                .isInstanceOf(ApiException.class)
                .hasMessage("系统内置配置不可删除");
    }

    @Test
    void deletesCustomSettings() {
        SystemSettingMapper systemSettingMapper = mock(SystemSettingMapper.class);
        DatabaseResourceRepository repository = repository(systemSettingMapper);
        when(systemSettingMapper.selectById(11L)).thenReturn(setting(11L, "theme.color", "主题颜色", "blue", false));

        repository.delete(resource("settings"), 11L);

        verify(systemSettingMapper).deleteById(11L);
    }

    @Test
    void delegatesDynamicResourcesToDynamicTableService() {
        DynamicTableService dynamicTableService = mock(DynamicTableService.class);
        DatabaseResourceRepository repository = repository(mock(SystemSettingMapper.class), dynamicTableService);
        ResourceDefinition resource = resource("repair_order");
        Map<String, Object> row = Map.of("id", 1L, "title", "报修");
        when(dynamicTableService.create(resource, Map.of("title", "报修"))).thenReturn(row);

        Map<String, Object> created = repository.create(resource, Map.of("title", "报修"));

        assertThat(created).isEqualTo(row);
        verify(dynamicTableService).create(resource, Map.of("title", "报修"));
    }

    @Test
    void pagesApplicationsThroughDatabaseMapper() {
        ApplicationMapper applicationMapper = mock(ApplicationMapper.class);
        FaqMapper faqMapper = mock(FaqMapper.class);
        DatabaseResourceRepository repository = new DatabaseResourceRepository(
                mock(IAdminUserService.class),
                applicationMapper,
                faqMapper,
                mock(DictMapper.class),
                mock(AuditLogMapper.class),
                mock(SystemSettingMapper.class),
                mock(MenuMapper.class),
                new ResourceStructMapper(),
                mock(DynamicTableService.class)
        );
        ResourceDefinition resource = new ResourceDefinition(
                "applications",
                "/applications",
                "租房申请",
                "application",
                "id",
                List.of("applicant", "phone", "property"),
                List.of("status"),
                List.of("applicant", "phone", "property", "moveInDate", "leasePeriod", "status"),
                List.of("id", "applicant", "phone", "property", "moveInDate", "leasePeriod", "status"),
                List.of(),
                List.of(),
                List.of()
        );
        ApplicationDO application = new ApplicationDO();
        application.setId(8L);
        application.setApplicant("张三");
        application.setPhone("13800000000");
        application.setProperty("阳光花园");
        application.setStatus("pending");
        Map<String, String> params = Map.of("pageNum", "2", "pageSize", "5", "applicant", "张");
        when(applicationMapper.selectPage(params, resource.searchableFields(), 2, 5)).thenReturn(List.of(application));
        when(applicationMapper.count(params, resource.searchableFields())).thenReturn(12L);

        PageResult page = repository.page(resource, params);

        assertThat(page.total()).isEqualTo(12L);
        assertThat(page.list()).hasSize(1);
        assertThat(page.list().get(0))
                .containsEntry("id", 8L)
                .containsEntry("applicant", "张三");
        verify(applicationMapper).selectPage(params, resource.searchableFields(), 2, 5);
        verify(applicationMapper).count(params, resource.searchableFields());
        verifyNoInteractions(faqMapper);
    }

    @Test
    void rejectsDeletingBuiltInMenus() {
        MenuMapper menuMapper = mock(MenuMapper.class);
        DatabaseResourceRepository repository = new DatabaseResourceRepository(
                mock(IAdminUserService.class),
                mock(ApplicationMapper.class),
                mock(FaqMapper.class),
                mock(DictMapper.class),
                mock(AuditLogMapper.class),
                mock(SystemSettingMapper.class),
                menuMapper,
                new ResourceStructMapper(),
                mock(DynamicTableService.class)
        );
        MenuDO menu = new MenuDO();
        menu.setId(9L);
        menu.setMenuKey("system");
        menu.setLabel("menu.system");
        menu.setIcon("cog");
        menu.setSystemBuiltin(true);
        when(menuMapper.selectById(9L)).thenReturn(menu);

        assertThatThrownBy(() -> repository.delete(resource("menus"), 9L))
                .isInstanceOf(ApiException.class)
                .hasMessage("系统内置菜单不可删除");
    }

    @Test
    void pagesAdminsThroughAdminUserService() {
        IAdminUserService adminUserService = mock(IAdminUserService.class);
        DatabaseResourceRepository repository = new DatabaseResourceRepository(
                adminUserService,
                mock(ApplicationMapper.class),
                mock(FaqMapper.class),
                mock(DictMapper.class),
                mock(AuditLogMapper.class),
                mock(SystemSettingMapper.class),
                mock(MenuMapper.class),
                new ResourceStructMapper(),
                mock(DynamicTableService.class)
        );
        ResourceDefinition resource = resource("admins");
        Map<String, String> params = Map.of("pageNum", "1", "pageSize", "10", "name", "管");
        Map<String, Object> row = Map.of("id", 1L, "username", "admin", "name", "管理员", "role", "admin", "status", "enabled");
        when(adminUserService.listAdmins(params)).thenReturn(new PageResult(List.of(row), 1));

        PageResult page = repository.page(resource, params);

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.list()).containsExactly(row);
    }

    @Test
    void mapsAdminCreateUpdateAndDeletePayloads() {
        IAdminUserService adminUserService = mock(IAdminUserService.class);
        DatabaseResourceRepository repository = new DatabaseResourceRepository(
                adminUserService,
                mock(ApplicationMapper.class),
                mock(FaqMapper.class),
                mock(DictMapper.class),
                mock(AuditLogMapper.class),
                mock(SystemSettingMapper.class),
                mock(MenuMapper.class),
                new ResourceStructMapper(),
                mock(DynamicTableService.class)
        );
        ResourceDefinition resource = resource("admins");
        when(adminUserService.createAdmin(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminUserDTO(8L, "ops", "运营", "admin", true, null, null));
        when(adminUserService.updateAdmin(org.mockito.ArgumentMatchers.eq(8L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminUserDTO(8L, "ops", "运营主管", "operator", false, null, null));

        Map<String, Object> created = repository.create(resource, Map.of(
                "username", "ops",
                "password", "Aa123456!",
                "name", "运营",
                "role", "admin"
        ));
        Map<String, Object> updated = repository.update(resource, 8L, Map.of(
                "name", "运营主管",
                "role", "operator",
                "status", "disabled"
        ));
        repository.delete(resource, 8L);

        assertThat(created)
                .containsEntry("id", 8L)
                .containsEntry("role", "admin")
                .containsEntry("status", "enabled");
        assertThat(updated)
                .containsEntry("name", "运营主管")
                .containsEntry("role", "operator")
                .containsEntry("status", "disabled");
        verify(adminUserService).deleteAdmin(8L);
    }

    private SystemSettingDO setting(Long id, String key, String name, String value, boolean systemBuiltin) {
        SystemSettingDO setting = new SystemSettingDO();
        setting.setId(id);
        setting.setSettingKey(key);
        setting.setSettingName(name);
        setting.setSettingValue(value);
        setting.setDescription("说明");
        setting.setSystemBuiltin(systemBuiltin);
        setting.setCreateTime(LocalDateTime.of(2026, 1, 1, 12, 0));
        return setting;
    }

    private DatabaseResourceRepository repository(SystemSettingMapper systemSettingMapper) {
        return repository(systemSettingMapper, mock(DynamicTableService.class));
    }

    private DatabaseResourceRepository repository(SystemSettingMapper systemSettingMapper, DynamicTableService dynamicTableService) {
        return new DatabaseResourceRepository(
                mock(IAdminUserService.class),
                mock(ApplicationMapper.class),
                mock(FaqMapper.class),
                mock(DictMapper.class),
                mock(AuditLogMapper.class),
                systemSettingMapper,
                mock(MenuMapper.class),
                new ResourceStructMapper(),
                dynamicTableService
        );
    }

    private ResourceDefinition resource(String key) {
        return new ResourceDefinition(
                key,
                "/" + key,
                key,
                key,
                "id",
                List.of(),
                List.of(),
                List.of("title"),
                List.of("title", "status"),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
