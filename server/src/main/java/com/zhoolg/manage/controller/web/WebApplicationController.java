package com.zhoolg.manage.controller.web;

import com.zhoolg.manage.service.IAuthService;
import com.zhoolg.manage.entity.dto.ApplicationSubmitDTO;
import com.zhoolg.manage.entity.base.ApiResponse;
import com.zhoolg.manage.entity.base.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/web/applications")
public class WebApplicationController {
    private final IAuthService authService;

    public WebApplicationController(IAuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> submit(
            @Valid @RequestBody ApplicationSubmitDTO request
    ) {
        // 可选鉴权：存在有效 Cookie 会话则绑定用户，否则按匿名提交处理。
        Long userId = null;
        try {
            CurrentUser user = authService.requireUser();
            userId = user.id();
        } catch (Exception ignored) {
            // 忽略无效或缺失会话，允许匿名提交。
        }

        return ApiResponse.ok(Map.of(
                "id", System.currentTimeMillis(),
                "status", "pending",
                "submitTime", LocalDateTime.now().toString(),
                "userId", userId,
                "propertyId", request.propertyId(),
                "contactName", request.contactName(),
                "contactPhone", request.contactPhone()
        ), "申请已提交");
    }

    @GetMapping("/my")
    public ApiResponse<List<Map<String, Object>>> myApplications() {
        CurrentUser user = authService.requireUser();

        // TODO: 从数据库查询当前用户的申请记录
        return ApiResponse.ok(List.of(Map.of(
                "id", 1,
                "property", "阳光花园 2 室 1 厅",
                "status", "pending",
                "createTime", "2026-05-31 10:00:00",
                "userId", user.id()
        )));
    }
}
