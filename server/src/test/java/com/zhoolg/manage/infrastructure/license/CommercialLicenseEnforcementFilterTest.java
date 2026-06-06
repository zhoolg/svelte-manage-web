package com.zhoolg.manage.infrastructure.license;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.infrastructure.license.integrity.RuntimeIntegrityStatus;
import com.zhoolg.manage.infrastructure.license.integrity.RuntimeIntegrityVerifier;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommercialLicenseEnforcementFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unauthenticatedRequestDelegatesToSecurityEntryPoint() throws Exception {
        AtomicBoolean licenseChecked = new AtomicBoolean(false);
        CommercialLicenseService licenseService = mock(CommercialLicenseService.class);
        RuntimeIntegrityVerifier runtimeIntegrityVerifier = mock(RuntimeIntegrityVerifier.class);
        when(licenseService.status()).thenAnswer(invocation -> {
            licenseChecked.set(true);
            return CommercialLicenseStatus.blocked("missing", "", "未检测到商业授权文件");
        });
        when(runtimeIntegrityVerifier.verify()).thenReturn(RuntimeIntegrityStatus.trusted("ok", "ok"));
        CommercialLicenseEnforcementFilter filter = new CommercialLicenseEnforcementFilter(
                licenseService,
                runtimeIntegrityVerifier,
                new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/meta/modules");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        assertThat(licenseChecked).isFalse();
    }

    @Test
    void authenticatedBusinessRequestRequiresCommercialLicense() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "admin:view")
        );
        CommercialLicenseService licenseService = mock(CommercialLicenseService.class);
        RuntimeIntegrityVerifier runtimeIntegrityVerifier = mock(RuntimeIntegrityVerifier.class);
        when(licenseService.status()).thenReturn(
                CommercialLicenseStatus.blocked("missing", "", "未检测到商业授权文件")
        );
        when(runtimeIntegrityVerifier.verify()).thenReturn(RuntimeIntegrityStatus.trusted("ok", "ok"));
        CommercialLicenseEnforcementFilter filter = new CommercialLicenseEnforcementFilter(
                licenseService,
                runtimeIntegrityVerifier,
                new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/meta/modules");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(response.getContentAsString()).contains("未检测到商业授权文件");
    }

    @Test
    void authenticatedBusinessRequestRequiresTrustedRuntime() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "n/a", "admin:view")
        );
        CommercialLicenseService licenseService = mock(CommercialLicenseService.class);
        RuntimeIntegrityVerifier runtimeIntegrityVerifier = mock(RuntimeIntegrityVerifier.class);
        when(licenseService.status()).thenReturn(
                CommercialLicenseStatus.allowed("ok", "", "ok", "", "", "", null)
        );
        when(runtimeIntegrityVerifier.verify()).thenReturn(
                RuntimeIntegrityStatus.untrusted("tampered", "运行库被篡改")
        );
        CommercialLicenseEnforcementFilter filter = new CommercialLicenseEnforcementFilter(
                licenseService,
                runtimeIntegrityVerifier,
                new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/meta/modules");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(response.getContentAsString()).contains("官方运行库完整性校验失败");
    }
}
