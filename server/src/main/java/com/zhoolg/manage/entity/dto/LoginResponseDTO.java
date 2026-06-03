package com.zhoolg.manage.entity.dto;

import java.util.List;

public record LoginResponseDTO(
        String token,
        UserPayload user,
        List<String> permissions,
        boolean isAdmin,
        boolean usingDefaultPassword
) {
    public record UserPayload(long id, String username, String name, String avatar, List<String> roles, boolean usingDefaultPassword) {
    }
}
