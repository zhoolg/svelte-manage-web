package com.zhoolg.manage.controller.admin;

import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.infrastructure.auth.CaptchaService;
import com.zhoolg.manage.infrastructure.auth.CryptoService;
import com.zhoolg.manage.infrastructure.auth.RateLimitService;
import com.zhoolg.manage.service.PasskeyService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void captchaGenerationIsRateLimitedByClientIp() {
        CaptchaService captchaService = mock(CaptchaService.class);
        RateLimitService rateLimitService = mock(RateLimitService.class);
        AuthController controller = new AuthController(
                null,
                null,
                mock(PasskeyService.class),
                mock(CryptoService.class),
                captchaService,
                rateLimitService
        );
        ReflectionTestUtils.setField(controller, "captchaRateLimitPerMinute", 20);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/auth/captcha");
        request.setRemoteAddr("10.0.0.8");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(rateLimitService.overLimit(eq("auth:captcha:10.0.0.8"), eq(20), eq(Duration.ofMinutes(1))))
                .thenReturn(true);

        assertThatThrownBy(() -> controller.captcha(request, response))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(429))
                .hasMessage("验证码请求过于频繁，请稍后再试");
        verify(captchaService, never()).generate();
    }
}
