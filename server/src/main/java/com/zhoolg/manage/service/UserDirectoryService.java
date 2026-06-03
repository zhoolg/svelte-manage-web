package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.pojo.AdminUserDO;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.mapper.AdminUserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 账号目录服务：管理员账号存储在 sys_admin_user 表中。
 * 提供统一的账号查询和管理接口，供 AuthServiceImpl 和 AdminUserServiceImpl 使用。
 */
@Service
public class UserDirectoryService {

    /**
     * 账号快照。passwordHash 可能为空（尚未播种）；enabled=false 表示禁用，不允许登录。
     */
    public record Account(Long id, String loginName, String passwordHash, String name, String avatar, String roleCode, boolean enabled,
                          LocalDateTime createTime, LocalDateTime updateTime) {
    }

    private final AdminUserMapper mapper;

    public UserDirectoryService(AdminUserMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<Account> findByLoginName(String loginName) {
        if (loginName == null || loginName.isBlank()) {
            return Optional.empty();
        }
        return mapper.findByUsername(loginName.trim())
                .map(this::toAccount);
    }

    public Optional<Account> findById(Long id) {
        return mapper.findById(id)
                .map(this::toAccount);
    }

    public List<Account> findAll() {
        return mapper.findAll().stream()
                .map(this::toAccount)
                .toList();
    }

    public Account create(Account account) {
        if (mapper.existsByUsername(account.loginName())) {
            throw new ApiException(400, "用户名已存在");
        }

        AdminUserDO entity = new AdminUserDO();
        entity.setUsername(account.loginName());
        entity.setPasswordHash(account.passwordHash() != null ? account.passwordHash() : "");
        entity.setName(account.name() != null ? account.name() : "");
        entity.setAvatar(normalizeAvatar(account.avatar()));
        entity.setRoleCode(account.roleCode() != null ? account.roleCode() : "viewer");
        entity.setEnabled(account.enabled());

        AdminUserDO saved = mapper.save(entity);
        return toAccount(saved);
    }

    public void update(Account account) {
        AdminUserDO entity = mapper.findById(account.id())
                .orElseThrow(() -> new ApiException(404, "账号不存在"));

        entity.setUsername(account.loginName());
        entity.setPasswordHash(account.passwordHash() != null ? account.passwordHash() : "");
        entity.setName(account.name() != null ? account.name() : "");
        entity.setAvatar(normalizeAvatar(account.avatar()));
        entity.setRoleCode(account.roleCode() != null ? account.roleCode() : "viewer");
        entity.setEnabled(account.enabled());

        mapper.save(entity);
    }

    public void delete(Long id) {
        if (!mapper.existsById(id)) {
            throw new ApiException(404, "账号不存在");
        }
        mapper.deleteById(id);
    }

    /**
     * 将密码哈希写回指定账号（用于启动时播种密码）
     */
    public void savePasswordHash(Long id, String passwordHash) {
        AdminUserDO entity = mapper.findById(id)
                .orElseThrow(() -> new ApiException(404, "账号不存在"));
        entity.setPasswordHash(passwordHash);
        mapper.save(entity);
    }

    private Account toAccount(AdminUserDO entity) {
        return new Account(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getName(),
                entity.getAvatar(),
                entity.getRoleCode(),
                entity.getEnabled(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }

    private String normalizeAvatar(String avatar) {
        return avatar == null || avatar.isBlank() ? null : avatar.trim();
    }
}
