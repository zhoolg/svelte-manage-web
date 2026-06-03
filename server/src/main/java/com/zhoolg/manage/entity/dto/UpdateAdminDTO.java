package com.zhoolg.manage.entity.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * 更新管理员请求 DTO
 */
public record UpdateAdminDTO(
        @Size(max = 64, message = "姓名最长 64 字符")
        String name,

        @Pattern(regexp = "^(super_admin|admin|operator|viewer)$", message = "角色必须为 super_admin/admin/operator/viewer")
        String roleCode,

        Boolean enabled
) {
}
