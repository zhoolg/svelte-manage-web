package com.zhoolg.manage.controller.web;

import com.zhoolg.manage.service.IAuthService;
import com.zhoolg.manage.service.IAdminUserService;
import com.zhoolg.manage.service.ICrudService;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.entity.base.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Web 端（小程序/H5）认证控制器
 *
 * 微信登录必须接入真实微信 API 校验 code 后才能启用：
 * 1. 调用微信 code2Session API 校验 code 并获取 openid
 * 2. 用 openid 查找/创建用户
 * 3. 签发 httpOnly Cookie 会话（复用 AuthServiceImpl）
 */
@RestController
@RequestMapping("/web/auth")
public class WebAuthController {

    @PostMapping("/wechat-login")
    public ApiResponse<Map<String, Object>> wechatLogin(@RequestBody Map<String, String> payload) {
        throw new ApiException(501, "微信登录尚未接入真实 code2Session 校验，已拒绝演示登录");
    }
}
