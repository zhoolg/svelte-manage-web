package com.zhoolg.manage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.entity.base.CurrentUser;
import com.zhoolg.manage.entity.pojo.AuditLogDO;
import com.zhoolg.manage.mapper.AuditLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditLogServiceTest {

    @Test
    void recordsStructuredAuditLogWithoutJsonResourceMirror() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        AuditLogService service = new AuditLogService(auditLogMapper, new ObjectMapper());
        CurrentUser user = new CurrentUser(7L, "admin", "管理员", "super_admin", List.of("*"), true);

        service.success(user, "ai", "apply", "ai_task", "AI-001", "应用 AI 模块");

        ArgumentCaptor<AuditLogDO> auditCaptor = ArgumentCaptor.forClass(AuditLogDO.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getUsername()).isEqualTo("admin");
        assertThat(auditCaptor.getValue().getModule()).isEqualTo("ai");
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("apply");
        assertThat(auditCaptor.getValue().getResult()).isEqualTo("success");
    }
}
