package com.zhoolg.manage.infrastructure.crud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.entity.base.CurrentUser;
import com.zhoolg.manage.entity.base.PageResult;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.service.AuditLogService;
import com.zhoolg.manage.service.IAuthService;
import com.zhoolg.manage.service.ICrudService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestCrudControllerDynamicContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatedDynamicModuleHttpCrudContractCoversRuntimeWorkflow() throws Exception {
        ResourceDefinition resource = resource();
        ResourceRegistry registry = mock(ResourceRegistry.class);
        IAuthService authService = mock(IAuthService.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        InMemoryCrudService crudService = new InMemoryCrudService();
        CurrentUser user = new CurrentUser(
                1L,
                "admin",
                "管理员",
                "admin",
                List.of("repair_order:*"),
                true
        );
        when(registry.require("repair_order")).thenReturn(resource);
        when(authService.requireUser("Bearer test-token")).thenReturn(user);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new RestCrudController(registry, crudService, authService, auditLogService)
        ).build();

        mockMvc.perform(get("/rest/repair_order")
                        .header("Authorization", "Bearer test-token")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(post("/rest/repair_order")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "水管维修", "status", "draft"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("水管维修"))
                .andExpect(jsonPath("$.data.status").value("draft"));

        mockMvc.perform(put("/rest/repair_order/1")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "厨房水管维修"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("厨房水管维修"));

        mockMvc.perform(post("/rest/repair_order/1/status")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("field", "status", "value", "submitted"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("submitted"));

        mockMvc.perform(post("/rest/repair_order/1/workflow/assign")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("assigned"));
        verify(auditLogService).success(
                eq(user),
                eq("repair_order"),
                eq("workflow:assign"),
                eq("dynamic_resource"),
                eq("1"),
                eq("执行工作流动作 assign")
        );

        mockMvc.perform(delete("/rest/repair_order/1")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));

        mockMvc.perform(get("/rest/repair_order")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        verify(authService, org.mockito.Mockito.times(2)).requirePermission(user, "repair_order:view");
        verify(authService).requirePermission(user, "repair_order:add");
        verify(authService, org.mockito.Mockito.times(3)).requirePermission(user, "repair_order:edit");
        verify(authService).requirePermission(user, "repair_order:delete");
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private ResourceDefinition resource() {
        return new ResourceDefinition(
                "repair_order",
                "/repair_order",
                "报修管理",
                "repair_order",
                "id",
                List.of("title"),
                List.of("status"),
                List.of("title", "status"),
                List.of("title", "status"),
                List.of(
                        Map.of("field", "id", "label", "ID"),
                        Map.of("field", "title", "label", "标题"),
                        Map.of("field", "status", "label", "状态")
                ),
                List.of(Map.of("field", "title", "label", "标题", "type", "input")),
                List.of(
                        Map.of("field", "title", "label", "标题", "required", true),
                        Map.of("field", "status", "label", "状态", "required", true)
                ),
                List.of(Map.of(
                        "action", "assign",
                        "from", "submitted",
                        "to", "assigned",
                        "statusField", "status"
                ))
        );
    }

    private static class InMemoryCrudService implements ICrudService {
        private final AtomicLong idGenerator = new AtomicLong(1);
        private final Map<Long, Map<String, Object>> rows = new LinkedHashMap<>();

        @Override
        public PageResult page(ResourceDefinition resource, Map<String, String> params) {
            return new PageResult(new ArrayList<>(rows.values()), rows.size());
        }

        @Override
        public Map<String, Object> create(ResourceDefinition resource, Map<String, Object> payload) {
            long id = idGenerator.getAndIncrement();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", id);
            row.putAll(payload);
            rows.put(id, row);
            return row;
        }

        @Override
        public Map<String, Object> update(ResourceDefinition resource, Object id, Map<String, Object> payload) {
            Map<String, Object> row = require(id);
            payload.forEach((key, value) -> {
                if (!"id".equals(key)) {
                    row.put(key, value);
                }
            });
            return row;
        }

        @Override
        public void delete(ResourceDefinition resource, Object id) {
            rows.remove(parseId(id));
        }

        @Override
        public void batchDelete(ResourceDefinition resource, List<?> ids) {
            ids.forEach(id -> rows.remove(parseId(id)));
        }

        @Override
        public Map<String, Object> updateStatus(ResourceDefinition resource, Object id, Map<String, Object> payload) {
            String field = String.valueOf(payload.getOrDefault("field", "status"));
            Object value = payload.containsKey("value") ? payload.get("value") : payload.get(field);
            return update(resource, id, Map.of(field, value));
        }

        @Override
        public Map<String, Object> transitionWorkflow(ResourceDefinition resource, Object id, String action) {
            Map<String, Object> transition = resource.workflow().stream()
                    .filter(item -> action.equals(String.valueOf(item.get("action"))))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(400, "工作流动作不存在"));
            Map<String, Object> row = require(id);
            String statusField = String.valueOf(transition.getOrDefault("statusField", "status"));
            String from = String.valueOf(transition.get("from"));
            if (!from.equals(String.valueOf(row.get(statusField)))) {
                throw new ApiException(400, "当前状态不允许执行该工作流动作");
            }
            row.put(statusField, transition.get("to"));
            return row;
        }

        private Map<String, Object> require(Object id) {
            Map<String, Object> row = rows.get(parseId(id));
            if (row == null) {
                throw new ApiException(404, "数据不存在");
            }
            return row;
        }

        private Long parseId(Object id) {
            return Long.valueOf(String.valueOf(id));
        }
    }
}
