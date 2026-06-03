package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.pojo.SystemSettingDO;
import com.zhoolg.manage.mapper.SystemSettingMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SystemSettingService {

    private final SystemSettingMapper systemSettingMapper;

    public SystemSettingService(SystemSettingMapper systemSettingMapper) {
        this.systemSettingMapper = systemSettingMapper;
    }

    public Optional<String> getSetting(String key) {
        SystemSettingDO setting = systemSettingMapper.selectByKey(key);
        return setting == null ? Optional.empty() : Optional.ofNullable(setting.getSettingValue());
    }

    public String getSetting(String key, String defaultValue) {
        return getSetting(key).orElse(defaultValue);
    }

    public int getIntSetting(String key, int defaultValue) {
        return getSetting(key).map(Integer::parseInt).orElse(defaultValue);
    }

    public boolean getBooleanSetting(String key, boolean defaultValue) {
        return getSetting(key).map(Boolean::parseBoolean).orElse(defaultValue);
    }
}
