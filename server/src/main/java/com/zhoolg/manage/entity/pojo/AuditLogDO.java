package com.zhoolg.manage.entity.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AuditLogDO {
    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String targetType;
    private String targetId;
    private String result;
    private String description;
    private String ip;
    private String userAgent;
    private String detailsJson;
    private LocalDateTime createTime;
}
