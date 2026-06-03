package com.zhoolg.manage.entity.dto;

import java.time.LocalDateTime;

/**
 * 管理员响应 DTO
 */
public record AdminUserDTO(
        Long id,
        String username,
        String name,
        String roleCode,
        Boolean enabled,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
