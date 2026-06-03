package com.zhoolg.manage.infrastructure.crud;

import com.zhoolg.manage.service.IAuthService;
import com.zhoolg.manage.service.ICrudService;
import com.zhoolg.manage.service.AuditLogService;
import com.zhoolg.manage.service.impl.AuthServiceImpl;
import com.zhoolg.manage.entity.base.CurrentUser;
import com.zhoolg.manage.entity.base.ApiResponse;
import com.zhoolg.manage.entity.base.PageResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/{resource}")
public class RestCrudController {
    private final ResourceRegistry registry;
    private final ICrudService crudService;
    private final IAuthService authService;
    private final AuditLogService auditLogService;

    public RestCrudController(
            ResourceRegistry registry,
            ICrudService crudService,
            IAuthService authService,
            AuditLogService auditLogService
    ) {
        this.registry = registry;
        this.crudService = crudService;
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageResult> page(
            @PathVariable String resource,
            @RequestParam Map<String, String> params,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResourceDefinition definition = registry.require(resource);
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("view"));
        return ApiResponse.ok(crudService.page(definition, params));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(
            @PathVariable String resource,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResourceDefinition definition = registry.require(resource);
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("add"));
        return ApiResponse.ok(crudService.create(definition, payload), "新增成功");
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(
            @PathVariable String resource,
            @PathVariable String id,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResourceDefinition definition = registry.require(resource);
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("edit"));
        return ApiResponse.ok(crudService.update(definition, id, payload), "更新成功");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Boolean>> delete(
            @PathVariable String resource,
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResourceDefinition definition = registry.require(resource);
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("delete"));
        crudService.delete(definition, id);
        return ApiResponse.ok(Map.of("success", true), "删除成功");
    }

    @DeleteMapping
    public ApiResponse<Map<String, Boolean>> batchDelete(
            @PathVariable String resource,
            @RequestBody Map<String, List<Object>> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResourceDefinition definition = registry.require(resource);
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("delete"));
        crudService.batchDelete(definition, payload.getOrDefault("ids", List.of()));
        return ApiResponse.ok(Map.of("success", true), "批量删除成功");
    }

    @PostMapping("/{id}/status")
    public ApiResponse<Map<String, Object>> updateStatus(
            @PathVariable String resource,
            @PathVariable String id,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResourceDefinition definition = registry.require(resource);
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("edit"));
        return ApiResponse.ok(crudService.updateStatus(definition, id, payload), "状态更新成功");
    }

    @PostMapping("/{id}/workflow/{action}")
    public ApiResponse<Map<String, Object>> transitionWorkflow(
            @PathVariable String resource,
            @PathVariable String id,
            @PathVariable String action,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResourceDefinition definition = registry.require(resource);
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("edit"));
        Map<String, Object> result = crudService.transitionWorkflow(definition, id, action);
        auditLogService.success(user, definition.key(), "workflow:" + action, "dynamic_resource", id, "执行工作流动作 " + action);
        return ApiResponse.ok(result, "工作流流转成功");
    }
}
