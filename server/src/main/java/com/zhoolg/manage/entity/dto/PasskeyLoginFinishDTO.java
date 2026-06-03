package com.zhoolg.manage.entity.dto;

import jakarta.validation.constraints.NotBlank;

public record PasskeyLoginFinishDTO(
        @NotBlank String requestId,
        @NotBlank String credentialJson
) {
}
