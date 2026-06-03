package com.zhoolg.manage.entity.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ApplicationDO {
    private Long id;
    private String applicant;
    private String phone;
    private String property;
    private String moveInDate;
    private String leasePeriod;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
