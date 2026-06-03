package com.zhoolg.manage.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 安全响应头过滤器
 * 为所有响应添加安全相关的 HTTP 头，防御常见 Web 攻击
 */
@Component
public class SecurityHeadersFilter implements Filter {

    private static final String API_CSP = "default-src 'none'; frame-ancestors 'none'; img-src 'self' data:; style-src 'self'";
    private static final String SWAGGER_CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self' 'unsafe-inline'",
            "script-src-elem 'self'",
            "style-src 'self' 'unsafe-inline'",
            "style-src-elem 'self' 'unsafe-inline'",
            "img-src 'self' data:",
            "font-src 'self' data:",
            "connect-src 'self'",
            "frame-ancestors 'self'"
    );

    @Value("${app.auth.cookie.secure:false}")
    private boolean cookieSecure;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 防止点击劫持（Clickjacking）
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // 防止 MIME 类型嗅探
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // XSS 保护（现代浏览器已内置，但保留兼容性）
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // HSTS：强制 HTTPS（仅在生产环境 HTTPS 下启用）
        if (cookieSecure) {
            httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        // Referrer 策略：限制 Referer 头泄露
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 权限策略：禁用不必要的浏览器特性
        httpResponse.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");

        // Swagger UI 需要加载本机脚本、样式和字体，不能使用 API 的极简 CSP。
        httpResponse.setHeader("Content-Security-Policy", resolveContentSecurityPolicy(httpRequest));
        httpResponse.setHeader("X-Permitted-Cross-Domain-Policies", "none");

        chain.doFilter(request, response);
    }

    private String resolveContentSecurityPolicy(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return API_CSP;
        }
        if (path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui/")
                || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/")) {
            return SWAGGER_CSP;
        }
        // 默认拒绝被其他页面嵌入资源；API 服务不承载内联脚本。
        return API_CSP;
    }
}
