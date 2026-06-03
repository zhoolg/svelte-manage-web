package com.zhoolg.manage.infrastructure.auth;

import com.zhoolg.manage.entity.base.CurrentUser;
import com.zhoolg.manage.service.IAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 将现有 Cookie 会话鉴权结果注入 Spring Security 上下文。
 * 控制器仍可调用 IAuthService.requireUser；后续可逐步改用 SecurityContext。
 */
public class CookieSessionAuthenticationFilter extends OncePerRequestFilter {

    private final IAuthService authService;

    public CookieSessionAuthenticationFilter(IAuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                CurrentUser user = authService.requireUser();
                CookieSessionAuthentication authentication = new CookieSessionAuthentication(user);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                // 未登录或无效会话时保持匿名，交由后续授权决策。
            }
        }
        filterChain.doFilter(request, response);
    }
}
