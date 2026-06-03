package com.zhoolg.manage.entity.dto;

import java.time.LocalDateTime;

public record PasskeyCredentialDTO(
        Long id,
        String displayName,
        String username,
        LocalDateTime lastUsedTime,
        LocalDateTime createTime
) {
}
