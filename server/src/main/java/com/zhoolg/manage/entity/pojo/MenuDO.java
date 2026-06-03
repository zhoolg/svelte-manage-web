package com.zhoolg.manage.entity.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MenuDO {
    private Long id;
    private String menuKey;
    private String parentKey;
    private String label;
    private String icon;
    private String path;
    private String moduleId;
    private String permissionCode;
    private Integer sortOrder;
    private Boolean hidden;
    private Boolean enabled;
    private Boolean systemBuiltin;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
