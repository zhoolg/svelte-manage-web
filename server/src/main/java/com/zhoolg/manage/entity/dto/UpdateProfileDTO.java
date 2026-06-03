package com.zhoolg.manage.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 当前用户资料更新请求 DTO。
 */
public record UpdateProfileDTO(
        @NotBlank(message = "姓名不能为空")
        @Size(max = 64, message = "姓名最长 64 字符")
        String name,

        @Size(max = 512, message = "头像地址最长 512 字符")
        String avatar
) {
}
