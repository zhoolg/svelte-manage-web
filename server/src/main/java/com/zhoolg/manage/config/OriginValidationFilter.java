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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cookie 会话下的跨站请求防护。
 * 对写请求执行 Origin/Referer 白名单校验，阻断跨站表单或脚本触发的状态变更。
 */
@Component
public class OriginValidationFilter implements Filter {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Value("${app.cors.allowed-origins:http://localhost:7052}")
    private String[] allowedOrigins;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!SAFE_METHODS.contains(httpRequest.getMethod())) {
            Set<String> allowed = allowedOriginSet();
            String requestOrigin = requestOrigin(httpRequest);
            if (requestOrigin == null || !allowed.contains(requestOrigin)) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden origin");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private Set<String> allowedOriginSet() {
        return Arrays.stream(allowedOrigins == null ? new String[0] : allowedOrigins)
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toSet());
    }

    private String requestOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            return origin.trim();
        }
        return refererOrigin(request.getHeader("Referer"));
    }

    private String refererOrigin(String referer) {
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(referer.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            int port = uri.getPort();
            String authority = port >= 0 ? uri.getHost() + ":" + port : uri.getHost();
            return uri.getScheme() + "://" + authority;
        } catch (URISyntaxException ex) {
            return null;
        }
    }
}
