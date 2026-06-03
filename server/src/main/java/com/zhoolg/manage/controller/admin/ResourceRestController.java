package com.zhoolg.manage.controller.admin;

import com.zhoolg.manage.service.IAuthService;
import com.zhoolg.manage.service.IAdminUserService;
import com.zhoolg.manage.service.ICrudService;
import com.zhoolg.manage.infrastructure.crud.ResourceDefinition;
import com.zhoolg.manage.infrastructure.crud.ResourceRegistry;
import com.zhoolg.manage.service.impl.AuthServiceImpl;
import com.zhoolg.manage.service.impl.CrudServiceImpl;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class ResourceRestController {
    private final ResourceRegistry registry;
    private final ICrudService crudService;
    private final IAuthService authService;

    public ResourceRestController(ResourceRegistry registry, ICrudService crudService, IAuthService authService) {
        this.registry = registry;
        this.crudService = crudService;
        this.authService = authService;
    }

    @GetMapping({"/applications", "/faq", "/logs", "/dict", "/settings", "/menus"})
    public ApiResponse<PageResult> page(
            @RequestParam Map<String, String> params,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        ResourceDefinition definition = definition(request);
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("view"));
        return ApiResponse.ok(crudService.page(definition, params));
    }

    @PostMapping({"/applications", "/faq", "/logs", "/dict", "/settings", "/menus"})
    public ApiResponse<Map<String, Object>> create(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        ResourceDefinition definition = definition(request);
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("add"));
        return ApiResponse.ok(crudService.create(definition, payload), "新增成功");
    }

    @PutMapping({"/applications/{id}", "/faq/{id}", "/logs/{id}", "/dict/{id}", "/settings/{id}", "/menus/{id}"})
    public ApiResponse<Map<String, Object>> update(
            @PathVariable String id,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        ResourceDefinition definition = definition(request);
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("edit"));
        return ApiResponse.ok(crudService.update(definition, id, payload), "更新成功");
    }

    @DeleteMapping({"/applications/{id}", "/faq/{id}", "/logs/{id}", "/dict/{id}", "/settings/{id}", "/menus/{id}"})
    public ApiResponse<Map<String, Boolean>> delete(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        ResourceDefinition definition = definition(request);
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("delete"));
        crudService.delete(definition, id);
        return ApiResponse.ok(Map.of("success", true), "删除成功");
    }

    @DeleteMapping({"/applications", "/faq", "/logs", "/dict", "/settings", "/menus"})
    public ApiResponse<Map<String, Boolean>> batchDelete(
            @RequestBody Map<String, List<Object>> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        ResourceDefinition definition = definition(request);
        CurrentUser user = authService.requireUser(authorization);
        authService.requirePermission(user, definition.permission("delete"));
        crudService.batchDelete(definition, payload.getOrDefault("ids", List.of()));
        return ApiResponse.ok(Map.of("success", true), "批量删除成功");
    }

    private ResourceDefinition definition(jakarta.servlet.http.HttpServletRequest request) {
        String path = request.getRequestURI();
        String key = path.split("/")[1];
        return registry.require(registry.normalizeKey(key));
    }
}
