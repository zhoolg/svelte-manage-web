package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.base.PageResult;
import com.zhoolg.manage.entity.dto.AiSmokeTestResult;
import com.zhoolg.manage.infrastructure.crud.ResourceDefinition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiRuntimeCrudSmokeTestServiceTest {

    @Test
    void runCreatesPagesUpdatesTransitionsAndDeletesDynamicResource() {
        ICrudService crudService = mock(ICrudService.class);
        AiRuntimeCrudSmokeTestService service = new AiRuntimeCrudSmokeTestService(crudService);
        ResourceDefinition resource = resource();
        when(crudService.create(eq(resource), any())).thenReturn(Map.of(
                "id", 7L,
                "reporter", "AI Smoke Test",
                "status", "pending"
        ));
        when(crudService.page(eq(resource), any())).thenReturn(new PageResult(new ArrayList<>(), 1));
        when(crudService.update(eq(resource), eq(7L), any())).thenReturn(Map.of("id", 7L));
        when(crudService.transitionWorkflow(resource, 7L, "approve")).thenReturn(Map.of("id", 7L, "status", "approved"));

        List<AiSmokeTestResult.Check> checks = service.run(resource);

        assertThat(checks)
                .extracting(AiSmokeTestResult.Check::code)
                .containsExactly(
                        "runtime_create",
                        "runtime_page",
                        "runtime_update",
                        "runtime_workflow",
                        "runtime_delete"
                );
        assertThat(checks)
                .extracting(AiSmokeTestResult.Check::status)
                .containsOnly("passed");

        ArgumentCaptor<Map<String, Object>> createPayload = ArgumentCaptor.forClass(Map.class);
        verify(crudService).create(eq(resource), createPayload.capture());
        assertThat(createPayload.getValue())
                .containsEntry("reporter", "AI Smoke Test")
                .containsEntry("status", "pending");

        ArgumentCaptor<Map<String, Object>> updatePayload = ArgumentCaptor.forClass(Map.class);
        verify(crudService).update(eq(resource), eq(7L), updatePayload.capture());
        assertThat(updatePayload.getValue()).containsEntry("reporter", "AI Smoke Test Updated");
        verify(crudService).delete(resource, 7L);
    }

    @Test
    void runReportsRuntimeCrudFailureWhenCreateThrows() {
        ICrudService crudService = mock(ICrudService.class);
        AiRuntimeCrudSmokeTestService service = new AiRuntimeCrudSmokeTestService(crudService);
        ResourceDefinition resource = resource();
        when(crudService.create(eq(resource), any())).thenThrow(new IllegalStateException("table missing"));

        List<AiSmokeTestResult.Check> checks = service.run(resource);

        assertThat(checks)
                .extracting(AiSmokeTestResult.Check::code)
                .containsExactly("runtime_crud");
        assertThat(checks.get(0).message()).contains("table missing");
        verify(crudService, never()).delete(eq(resource), any());
    }

    private ResourceDefinition resource() {
        return new ResourceDefinition(
                "repair_order",
                "/rest/repair_order",
                "报修管理",
                "repair_order",
                "id",
                List.of("reporter"),
                List.of("status"),
                List.of("reporter", "status"),
                List.of("reporter", "status"),
                List.of(Map.of("field", "reporter"), Map.of("field", "status")),
                List.of(Map.of("field", "reporter", "type", "input")),
                List.of(
                        Map.of("field", "reporter", "type", "input", "required", true),
                        Map.of(
                                "field", "status",
                                "type", "select",
                                "options", List.of(
                                        Map.of("label", "待审核", "value", "pending"),
                                        Map.of("label", "已通过", "value", "approved")
                                )
                        )
                ),
                List.of(Map.of(
                        "action", "approve",
                        "from", "pending",
                        "to", "approved",
                        "statusField", "status"
                ))
        );
    }
}
