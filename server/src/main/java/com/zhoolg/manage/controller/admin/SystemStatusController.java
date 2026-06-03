package com.zhoolg.manage.controller.admin;

import com.zhoolg.manage.entity.base.ApiResponse;
import com.zhoolg.manage.entity.dto.SystemStatusDTO;
import com.zhoolg.manage.service.IAuthService;
import com.zhoolg.manage.service.SystemStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/system")
public class SystemStatusController {
    private final SystemStatusService systemStatusService;
    private final IAuthService authService;

    public SystemStatusController(SystemStatusService systemStatusService, IAuthService authService) {
        this.systemStatusService = systemStatusService;
        this.authService = authService;
    }

    @GetMapping("/status")
    public ApiResponse<SystemStatusDTO> status() {
        authService.requireUser();
        return ApiResponse.ok(systemStatusService.snapshot());
    }
}
