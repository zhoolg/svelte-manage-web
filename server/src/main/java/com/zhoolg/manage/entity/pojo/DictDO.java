package com.zhoolg.manage.entity.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class DictDO {
    private Long id;
    private String name;
    private String code;
    private String value;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
