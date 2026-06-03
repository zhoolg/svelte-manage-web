package com.zhoolg.manage.entity.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AiGenerationTaskDO {
    private Long id;
    private String taskNo;
    private String prompt;
    private String moduleKey;
    private String moduleName;
    private String status;
    private String generatedSchema;
    private String generatedFiles;
    private String smokeTestStatus;
    private String smokeTestResult;
    private LocalDateTime smokeTestTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
