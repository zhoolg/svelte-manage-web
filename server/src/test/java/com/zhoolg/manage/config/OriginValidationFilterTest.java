package com.zhoolg.manage.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class OriginValidationFilterTest {
    @Test
    void rejectsUnsafeRequestFromForbiddenOrigin() throws ServletException, IOException {
        OriginValidationFilter filter = filter("http://localhost:7052");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admins");
        request.addHeader("Origin", "https://evil.example");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void rejectsUnsafeRequestWithoutOriginOrReferer() throws ServletException, IOException {
        OriginValidationFilter filter = filter("http://localhost:7052");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admins");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void allowsUnsafeRequestFromAllowedOrigin() throws ServletException, IOException {
        OriginValidationFilter filter = filter("http://localhost:7052");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admins");
        request.addHeader("Origin", "http://localhost:7052");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void allowsUnsafeRequestFromAllowedRefererWhenOriginMissing() throws ServletException, IOException {
        OriginValidationFilter filter = filter("http://localhost:7052");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admins");
        request.addHeader("Referer", "http://localhost:7052/admins");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void allowsSafeRequestWithoutOrigin() throws ServletException, IOException {
        OriginValidationFilter filter = filter("http://localhost:7052");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/meta/modules");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    private OriginValidationFilter filter(String... allowedOrigins) {
        OriginValidationFilter filter = new OriginValidationFilter();
        ReflectionTestUtils.setField(filter, "allowedOrigins", allowedOrigins);
        return filter;
    }
}
