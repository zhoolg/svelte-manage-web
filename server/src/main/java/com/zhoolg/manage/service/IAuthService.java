package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.LoginRequestDTO;
import com.zhoolg.manage.entity.dto.LoginResponseDTO;
import com.zhoolg.manage.entity.base.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 认证服务接口
 */
public interface IAuthService {
    LoginResponseDTO login(LoginRequestDTO request, HttpServletResponse response);
    LoginResponseDTO loginPasskey(String username, HttpServletResponse response);
    void logout(HttpServletRequest request, HttpServletResponse response);
    CurrentUser requireUser(String ignoredAuthorization);
    CurrentUser requireUser();
    void requirePermission(CurrentUser user, String permission);
    long activeSessionCount();
}
