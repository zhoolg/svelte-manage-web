package com.zhoolg.manage.entity.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AiUserProviderConfigDO {
    private Long id;
    private Long userId;
    private String provider;
    private String model;
    private String baseUrl;
    private String apiKeyCiphertext;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
