package com.zhoolg.manage.controller.admin;

import com.zhoolg.manage.entity.base.ApiResponse;
import com.zhoolg.manage.entity.base.CurrentUser;
import com.zhoolg.manage.entity.base.PageResult;
import com.zhoolg.manage.entity.dto.AdminUserDTO;
import com.zhoolg.manage.entity.dto.CreateAdminDTO;
import com.zhoolg.manage.entity.dto.UpdateAdminDTO;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.service.AuditLogService;
import com.zhoolg.manage.service.IAdminUserService;
import com.zhoolg.manage.service.IAuthService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.util.Map;

/**
 * 管理员用户控制器
 * 提供管理员 CRUD 接口，替代通用 CRUD 框架的 /admins 路由
 */
@RestController
@RequestMapping("/admins")
public class AdminUserController {
    private final IAdminUserService adminUserService;
    private final IAuthService authService;
    private final AuditLogService auditLogService;

    public AdminUserController(IAdminUserService adminUserService, IAuthService authService, AuditLogService auditLogService) {
        this.adminUserService = adminUserService;
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    /**
     * 分页查询管理员
     */
    @GetMapping
    @PreAuthorize("hasAuthority('admin:view')")
    public ApiResponse<PageResult> list(
            @RequestParam Map<String, String> params,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        CurrentUser user = authService.requireUser(authorization);
        return ApiResponse.ok(adminUserService.listAdmins(params));
    }

    /**
     * 获取管理员详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:view')")
    public ApiResponse<AdminUserDTO> get(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        CurrentUser user = authService.requireUser(authorization);
        return ApiResponse.ok(adminUserService.getAdmin(id));
    }

    /**
     * 创建管理员
     */
    @PostMapping
    @PreAuthorize("hasAuthority('admin:add')")
    public ApiResponse<AdminUserDTO> create(
            @Valid @RequestBody CreateAdminDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        CurrentUser user = authService.requireUser(authorization);
        AdminUserDTO created = adminUserService.createAdmin(request);
        auditLogService.success(user, "admin", "create", "admin_user", created.id(), "创建管理员 " + created.username());
        return ApiResponse.ok(created, "创建成功");
    }

    /**
     * 更新管理员
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:edit')")
    public ApiResponse<AdminUserDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdminDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        CurrentUser user = authService.requireUser(authorization);
        if (user.id() == id && (Boolean.FALSE.equals(request.enabled()) || request.roleCode() != null)) {
            throw new ApiException(400, "不能修改当前登录账号的角色或启用状态");
        }
        AdminUserDTO updated = adminUserService.updateAdmin(id, request);
        auditLogService.success(user, "admin", "update", "admin_user", id, "更新管理员 " + updated.username());
        return ApiResponse.ok(updated, "更新成功");
    }

    /**
     * 删除管理员
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:delete')")
    public ApiResponse<Map<String, Boolean>> delete(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        CurrentUser user = authService.requireUser(authorization);
        ensureNotSelf(user, id, "不能删除当前登录账号");
        adminUserService.deleteAdmin(id);
        auditLogService.success(user, "admin", "delete", "admin_user", id, "删除管理员");
        return ApiResponse.ok(Map.of("success", true), "删除成功");
    }

    /**
     * 禁用管理员
     */
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('admin:edit')")
    public ApiResponse<Void> disable(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        CurrentUser user = authService.requireUser(authorization);
        ensureNotSelf(user, id, "不能禁用当前登录账号");
        adminUserService.disableAdmin(id);
        auditLogService.success(user, "admin", "disable", "admin_user", id, "禁用管理员");
        return ApiResponse.ok(null, "禁用成功");
    }

    /**
     * 启用管理员
     */
    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('admin:edit')")
    public ApiResponse<Void> enable(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        CurrentUser user = authService.requireUser(authorization);
        adminUserService.enableAdmin(id);
        auditLogService.success(user, "admin", "enable", "admin_user", id, "启用管理员");
        return ApiResponse.ok(null, "启用成功");
    }

    private void ensureNotSelf(CurrentUser user, Long targetId, String message) {
        if (user.id() == targetId) {
            throw new ApiException(400, message);
        }
    }
}
