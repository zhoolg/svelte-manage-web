package com.zhoolg.manage.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建管理员请求 DTO
 */
public record CreateAdminDTO(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度为 3-32 字符")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名仅允许字母、数字、下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 128, message = "密码长度为 8-128 字符")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
                message = "密码必须包含大小写字母、数字和特殊字符"
        )
        String password,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 64, message = "姓名最长 64 字符")
        String name,

        @NotBlank(message = "角色不能为空")
        @Pattern(regexp = "^(super_admin|admin|operator|viewer)$", message = "角色必须为 super_admin/admin/operator/viewer")
        String roleCode
) {
}
