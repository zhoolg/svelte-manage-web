package com.zhoolg.manage;

import com.zhoolg.manage.mapper.AdminUserMapper;
import com.zhoolg.manage.mapper.ApplicationMapper;
import com.zhoolg.manage.mapper.AiGenerationTaskMapper;
import com.zhoolg.manage.mapper.AuditLogMapper;
import com.zhoolg.manage.mapper.DictMapper;
import com.zhoolg.manage.mapper.DynamicModuleMapper;
import com.zhoolg.manage.mapper.FaqMapper;
import com.zhoolg.manage.mapper.MenuMapper;
import com.zhoolg.manage.mapper.RolePermissionMapper;
import com.zhoolg.manage.mapper.SystemSettingMapper;
import com.zhoolg.manage.exception.ApiErrorCodes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.crypto.generate-ephemeral-key=true",
        "spring.ai.model.chat=none",
        "spring.ai.chat.client.enabled=false",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class ManageServerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserMapper adminUserMapper;

    @MockBean
    private AiGenerationTaskMapper aiGenerationTaskMapper;

    @MockBean
    private AuditLogMapper auditLogMapper;

    @MockBean
    private DynamicModuleMapper dynamicModuleMapper;

    @MockBean
    private ApplicationMapper applicationMapper;

    @MockBean
    private FaqMapper faqMapper;

    @MockBean
    private DictMapper dictMapper;

    @MockBean
    private RolePermissionMapper rolePermissionMapper;

    @MockBean
    private SystemSettingMapper systemSettingMapper;

    @MockBean
    private MenuMapper menuMapper;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void protectedAdminApiReturnsDedicatedSessionExpiredCode() throws Exception {
        mockMvc.perform(get("/admin/meta/modules"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ApiErrorCodes.AUTH_SESSION_EXPIRED))
                .andExpect(jsonPath("$.msg").value("登录已过期，请重新登录"));
    }
}
