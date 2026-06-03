package com.zhoolg.manage.entity.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AiSchemaMigrationDO {
    private Long id;
    private String migrationKey;
    private String moduleKey;
    private String tableName;
    private String columnName;
    private String operationType;
    private String ddlSql;
    private String status;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
