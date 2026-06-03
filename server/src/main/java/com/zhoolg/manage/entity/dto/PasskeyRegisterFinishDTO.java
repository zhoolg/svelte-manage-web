package com.zhoolg.manage.entity.dto;

import jakarta.validation.constraints.NotBlank;

public record PasskeyRegisterFinishDTO(
        @NotBlank String requestId,
        @NotBlank String credentialJson,
        String displayName
) {
}
