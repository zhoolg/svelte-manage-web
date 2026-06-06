package com.zhoolg.manage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.infrastructure.auth.CookieSessionAuthenticationFilter;
import com.zhoolg.manage.entity.base.ApiResponse;
import com.zhoolg.manage.exception.ApiErrorCodes;
import com.zhoolg.manage.infrastructure.license.CommercialLicenseEnforcementFilter;
import com.zhoolg.manage.infrastructure.license.CommercialLicenseService;
import com.zhoolg.manage.infrastructure.license.integrity.RuntimeIntegrityVerifier;
import jakarta.servlet.http.HttpServletResponse;
import com.zhoolg.manage.service.IAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 安全配置（阶段性）：
 * - 关闭默认登录页 / HTTP Basic；
 * - 基于现有 Cookie 会话，注入 SecurityContext，便于后续细粒度授权；
 * - swagger/health/file 上传等开放入口依旧放行，管理端/业务端 API 需要认证。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            IAuthService authService,
            CommercialLicenseService licenseService,
            RuntimeIntegrityVerifier runtimeIntegrityVerifier,
            ObjectMapper objectMapper,
            @Value("${app.security.public-docs-enabled:false}") boolean publicDocsEnabled
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new CookieSessionAuthenticationFilter(authService), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(
                        new CommercialLicenseEnforcementFilter(licenseService, runtimeIntegrityVerifier, objectMapper),
                        CookieSessionAuthenticationFilter.class
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeApiError(response, objectMapper, HttpStatus.UNAUTHORIZED,
                                        ApiErrorCodes.AUTH_SESSION_EXPIRED, "登录已过期，请重新登录"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeApiError(response, objectMapper, HttpStatus.FORBIDDEN,
                                        ApiErrorCodes.ACCESS_DENIED, "无权限访问"))
                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    if (publicDocsEnabled) {
                        auth.requestMatchers(
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**"
                        ).permitAll();
                    }
                    auth.requestMatchers(
                            "/actuator/health", "/actuator/info",
                            "/admin/auth/captcha", "/admin/auth/public-key", "/admin/auth/login",
                            "/admin/auth/passkeys/assertion/options", "/admin/auth/passkeys/assertion/finish"
                    ).permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/uploads/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/admins/**").hasAuthority("admin:view");
                    auth.requestMatchers(HttpMethod.POST, "/admins").hasAuthority("admin:add");
                    auth.requestMatchers(HttpMethod.PUT, "/admins/**").hasAuthority("admin:edit");
                    auth.requestMatchers(HttpMethod.DELETE, "/admins/**").hasAuthority("admin:delete");
                    auth.requestMatchers("/admin/**", "/web/**", "/applications/**", "/faq/**", "/logs/**", "/dict/**", "/settings/**")
                            .authenticated();
                    auth.anyRequest().authenticated();
                });
        return http.build();
    }

    private static void writeApiError(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            HttpStatus status,
            int code,
            String message
    ) throws java.io.IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(code, message));
    }
}
