package com.zhoolg.manage.entity.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SystemSettingDO {
    private Long id;
    private String settingKey;
    private String settingName;
    private String settingValue;
    private String description;
    private boolean systemBuiltin;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
