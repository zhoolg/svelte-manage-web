package com.zhoolg.manage.entity.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class DynamicModuleVersionDO {
    private Long id;
    private String moduleKey;
    private String moduleName;
    private Integer versionNo;
    private String taskNo;
    private String schemaHash;
    private String metadataJson;
    private String schemaJson;
    private LocalDateTime createTime;
}
