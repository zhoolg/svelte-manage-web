package com.zhoolg.manage.infrastructure.crud;

import com.zhoolg.manage.service.IAuthService;
import com.zhoolg.manage.service.ICrudService;
import com.zhoolg.manage.service.impl.AuthServiceImpl;
import com.zhoolg.manage.entity.base.CurrentUser;
import com.zhoolg.manage.entity.base.ApiResponse;
import com.zhoolg.manage.entity.base.PageResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@RestController
public class FrontendCompatCrudController {
    private final ResourceRegistry registry;
    private final ICrudService crudService;
    private final IAuthService authService;

    public FrontendCompatCrudController(ResourceRegistry registry, ICrudService crudService, IAuthService authService) {
        this.registry = registry;
        this.crudService = crudService;
        this.authService = authService;
    }

    @GetMapping({"/{resource}/list", "/interlocution"})
    public ApiResponse<PageResult> list(
            @PathVariable(required = false) String resource,
            @RequestParam Map<String, String> params,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResourceDefinition definition = definition(resource);
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("view"));
        return ApiResponse.ok(crudService.page(definition, params));
    }

    @PostMapping({"/{resource}/add", "/interlocution"})
    public ApiResponse<Map<String, Object>> add(
            @PathVariable(required = false) String resource,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResourceDefinition definition = definition(resource);
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("add"));
        return ApiResponse.ok(crudService.create(definition, payload), "新增成功");
    }

    @PostMapping("/{resource}/update")
    public ApiResponse<Map<String, Object>> update(
            @PathVariable(required = false) String resource,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResourceDefinition definition = definition(resource);
        Object id = payload.get("id");
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("edit"));
        return ApiResponse.ok(crudService.update(definition, id, payload), "更新成功");
    }

    @PostMapping("/{resource}/delete/{id}")
    public ApiResponse<Map<String, Boolean>> delete(
            @PathVariable(required = false) String resource,
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResourceDefinition definition = definition(resource);
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("delete"));
        crudService.delete(definition, id);
        return ApiResponse.ok(Map.of("success", true), "删除成功");
    }

    @PostMapping("/{resource}/delete/batch")
    public ApiResponse<Map<String, Boolean>> batchDelete(
            @PathVariable(required = false) String resource,
            @RequestBody Map<String, List<Object>> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResourceDefinition definition = definition(resource);
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("delete"));
        crudService.batchDelete(definition, payload.getOrDefault("ids", List.of()));
        return ApiResponse.ok(Map.of("success", true), "批量删除成功");
    }

    @PostMapping("/{resource}/status")
    public ApiResponse<Map<String, Object>> status(
            @PathVariable(required = false) String resource,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResourceDefinition definition = definition(resource);
        Object id = payload.getOrDefault("id", payload.get("resourceId"));
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("edit"));
        return ApiResponse.ok(crudService.updateStatus(definition, id, payload), "状态更新成功");
    }

    @PostMapping("/{resource}/workflow/{action}")
    public ApiResponse<Map<String, Object>> workflow(
            @PathVariable(required = false) String resource,
            @PathVariable String action,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResourceDefinition definition = definition(resource);
        Object id = payload.getOrDefault("id", payload.get("resourceId"));
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("edit"));
        return ApiResponse.ok(crudService.transitionWorkflow(definition, id, action), "工作流流转成功");
    }

    private ResourceDefinition definition(String resource) {
        return registry.require(registry.normalizeKey(resource == null ? "interlocution" : resource));
    }
}
