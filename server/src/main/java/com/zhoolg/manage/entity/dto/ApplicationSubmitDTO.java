package com.zhoolg.manage.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 申请提交请求 DTO
 */
public record ApplicationSubmitDTO(
        @NotNull(message = "房源 ID 不能为空")
        Long propertyId,

        @NotBlank(message = "联系人姓名不能为空")
        @Size(max = 64, message = "联系人姓名最长 64 字符")
        String contactName,

        @NotBlank(message = "联系电话不能为空")
        @Size(max = 32, message = "联系电话最长 32 字符")
        @Pattern(regexp = "^[0-9+\\-()\\s]{6,32}$", message = "联系电话格式不正确")
        String contactPhone,

        @Size(max = 1000, message = "留言最长 1000 字符")
        String message
) {
}
