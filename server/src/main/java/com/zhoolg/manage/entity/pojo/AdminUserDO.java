package com.zhoolg.manage.entity.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 管理员用户实体
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminUserDO {
    private Long id;
    private String username;
    private String passwordHash;
    private String name;
    private String avatar;
    private String roleCode = "viewer";
    private Boolean enabled = true;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
