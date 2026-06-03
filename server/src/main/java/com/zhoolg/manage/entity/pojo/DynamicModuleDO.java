package com.zhoolg.manage.entity.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class DynamicModuleDO {
    private Long id;
    private String moduleKey;
    private String moduleName;
    private String taskNo;
    private Integer currentVersionNo;
    private String schemaHash;
    private String metadataJson;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
