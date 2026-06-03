package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.AdminUserDTO;
import com.zhoolg.manage.entity.dto.CreateAdminDTO;
import com.zhoolg.manage.entity.dto.UpdateAdminDTO;
import com.zhoolg.manage.entity.base.PageResult;

import java.util.Map;

/**
 * 管理员用户服务接口
 */
public interface IAdminUserService {
    AdminUserDTO createAdmin(CreateAdminDTO request);
    AdminUserDTO updateAdmin(Long id, UpdateAdminDTO request);
    void disableAdmin(Long id);
    void enableAdmin(Long id);
    PageResult listAdmins(Map<String, String> params);
    AdminUserDTO getAdmin(Long id);
    void deleteAdmin(Long id);
}
