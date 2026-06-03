package com.zhoolg.manage.entity.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        String username,
        String accountNo,
        @NotBlank String password,
        String captchaId,
        String captcha
) {
    public String loginName() {
        if (username != null && !username.isBlank()) {
            return username;
        }
        return accountNo;
    }
}
