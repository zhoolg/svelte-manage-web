package com.zhoolg.manage.entity.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record PasskeyStartResponseDTO(
        String requestId,
        JsonNode publicKey
) {
}
