package com.zhoolg.manage.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 当前用户修改密码请求 DTO。
 */
public record ChangePasswordDTO(
        @NotBlank(message = "旧密码不能为空")
        String oldPassword,

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 72, message = "新密码长度需为 6-72 个字符")
        String newPassword,

        @NotBlank(message = "确认密码不能为空")
        String confirmPassword
) {
}
