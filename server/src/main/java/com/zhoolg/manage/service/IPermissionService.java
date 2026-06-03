package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.base.CurrentUser;

import java.util.List;
import java.util.Map;

/**
 * 权限服务接口
 */
public interface IPermissionService {
    void requirePermission(CurrentUser user, String requiredPermission);
    boolean hasPermission(CurrentUser user, String requiredPermission);
    List<String> permissionsForRole(String roleCode);
    List<Map<String, Object>> permissionCatalog();
    List<Map<String, Object>> roleMetadata();
}
