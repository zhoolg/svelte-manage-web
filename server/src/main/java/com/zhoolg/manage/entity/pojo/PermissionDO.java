package com.zhoolg.manage.entity.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class PermissionDO {
    private Long id;
    private String permissionCode;
    private String moduleKey;
    private String moduleName;
    private String actionCode;
    private String actionName;
    private String source;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
