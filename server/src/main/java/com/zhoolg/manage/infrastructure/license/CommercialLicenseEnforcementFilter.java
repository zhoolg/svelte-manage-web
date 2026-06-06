package com.zhoolg.manage.infrastructure.license;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.entity.base.ApiResponse;
import com.zhoolg.manage.exception.ApiErrorCodes;
import com.zhoolg.manage.infrastructure.license.integrity.RuntimeIntegrityStatus;
import com.zhoolg.manage.infrastructure.license.integrity.RuntimeIntegrityVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class CommercialLicenseEnforcementFilter extends OncePerRequestFilter {
    private final CommercialLicenseService licenseService;
    private final RuntimeIntegrityVerifier runtimeIntegrityVerifier;
    private final ObjectMapper objectMapper;

    public CommercialLicenseEnforcementFilter(
            CommercialLicenseService licenseService,
            RuntimeIntegrityVerifier runtimeIntegrityVerifier,
            ObjectMapper objectMapper
    ) {
        this.licenseService = licenseService;
        this.runtimeIntegrityVerifier = runtimeIntegrityVerifier;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!shouldEnforce(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        RuntimeIntegrityStatus integrity = runtimeIntegrityVerifier.verify();
        if (!integrity.trusted()) {
            writeBlocked(response, "官方运行库完整性校验失败");
            return;
        }

        CommercialLicenseStatus status = licenseService.status();
        if (!status.allowed()) {
            writeBlocked(response, status.message());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldEnforce(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        if (path == null) {
            return false;
        }
        return path.startsWith("/admin/")
                || path.startsWith("/web/")
                || path.startsWith("/applications")
                || path.startsWith("/faq")
                || path.startsWith("/logs")
                || path.startsWith("/dict")
                || path.startsWith("/settings")
                || path.startsWith("/admins");
    }

    private void writeBlocked(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.fail(ApiErrorCodes.ACCESS_DENIED, message == null || message.isBlank()
                        ? "当前部署未通过商业授权校验"
                        : message));
    }
}
