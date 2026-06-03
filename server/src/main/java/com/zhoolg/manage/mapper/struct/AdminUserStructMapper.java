package com.zhoolg.manage.mapper.struct;

import com.zhoolg.manage.entity.dto.AdminUserDTO;
import com.zhoolg.manage.service.UserDirectoryService;
import org.springframework.stereotype.Component;

@Component
public class AdminUserStructMapper {

    public AdminUserDTO toDto(UserDirectoryService.Account account) {
        return new AdminUserDTO(
                account.id(),
                account.loginName(),
                account.name(),
                account.roleCode(),
                account.enabled(),
                account.createTime(),
                account.updateTime()
        );
    }
}
