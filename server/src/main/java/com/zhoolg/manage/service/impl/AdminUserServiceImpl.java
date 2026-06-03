package com.zhoolg.manage.service.impl;

import com.zhoolg.manage.entity.base.PageResult;
import com.zhoolg.manage.entity.dto.AdminUserDTO;
import com.zhoolg.manage.entity.dto.CreateAdminDTO;
import com.zhoolg.manage.entity.dto.UpdateAdminDTO;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.mapper.struct.AdminUserStructMapper;
import com.zhoolg.manage.service.IAdminUserService;
import com.zhoolg.manage.service.UserDirectoryService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 管理员用户服务
 * 职责：管理员 CRUD、密码管理、状态管理
 */
@Service
public class AdminUserServiceImpl implements IAdminUserService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserDirectoryService userDirectory;
    private final PasswordEncoder passwordEncoder;
    private final AdminUserStructMapper structMapper;

    public AdminUserServiceImpl(UserDirectoryService userDirectory, PasswordEncoder passwordEncoder, AdminUserStructMapper structMapper) {
        this.userDirectory = userDirectory;
        this.passwordEncoder = passwordEncoder;
        this.structMapper = structMapper;
    }

    /**
     * 创建管理员
     */
    @Transactional
    public AdminUserDTO createAdmin(CreateAdminDTO request) {
        // 1. 校验用户名唯一性
        if (userDirectory.findByLoginName(request.username()).isPresent()) {
            throw new ApiException(400, "用户名已存在");
        }

        // 2. 密码哈希
        String passwordHash = passwordEncoder.encode(request.password());

        // 3. 创建账号
        UserDirectoryService.Account account = new UserDirectoryService.Account(
                null,
                request.username(),
                passwordHash,
                request.name(),
                null,
                request.roleCode(),
                true,
                null,
                null
        );
        UserDirectoryService.Account created = userDirectory.create(account);

        return structMapper.toDto(created);
    }

    /**
     * 更新管理员
     */
    @Transactional
    public AdminUserDTO updateAdmin(Long id, UpdateAdminDTO request) {
        UserDirectoryService.Account account = userDirectory.findById(id)
                .orElseThrow(() -> new ApiException(404, "管理员不存在"));
        boolean willDisable = request.enabled() != null && !request.enabled();
        boolean willLoseSuperAdmin = request.roleCode() != null && !isSuperAdminRole(request.roleCode());
        if (willDisable || willLoseSuperAdmin) {
            ensureNotLastSuperAdmin(account);
        }

        UserDirectoryService.Account updated = new UserDirectoryService.Account(
                account.id(),
                account.loginName(),
                account.passwordHash(),
                request.name() != null ? request.name() : account.name(),
                account.avatar(),
                request.roleCode() != null ? request.roleCode() : account.roleCode(),
                request.enabled() != null ? request.enabled() : account.enabled(),
                account.createTime(),
                account.updateTime()
        );

        userDirectory.update(updated);
        return structMapper.toDto(updated);
    }

    /**
     * 禁用管理员
     */
    @Transactional
    public void disableAdmin(Long id) {
        UserDirectoryService.Account account = userDirectory.findById(id)
                .orElseThrow(() -> new ApiException(404, "管理员不存在"));

        if (!account.enabled()) {
            throw new ApiException(400, "管理员已禁用");
        }
        ensureNotLastSuperAdmin(account);

        UserDirectoryService.Account disabled = new UserDirectoryService.Account(
                account.id(),
                account.loginName(),
                account.passwordHash(),
                account.name(),
                account.avatar(),
                account.roleCode(),
                false,
                account.createTime(),
                account.updateTime()
        );
        userDirectory.update(disabled);
    }

    /**
     * 启用管理员
     */
    @Transactional
    public void enableAdmin(Long id) {
        UserDirectoryService.Account account = userDirectory.findById(id)
                .orElseThrow(() -> new ApiException(404, "管理员不存在"));

        if (account.enabled()) {
            throw new ApiException(400, "管理员已启用");
        }

        UserDirectoryService.Account enabled = new UserDirectoryService.Account(
                account.id(),
                account.loginName(),
                account.passwordHash(),
                account.name(),
                account.avatar(),
                account.roleCode(),
                true,
                account.createTime(),
                account.updateTime()
        );
        userDirectory.update(enabled);
    }

    /**
     * 分页查询管理员
     */
    public PageResult listAdmins(Map<String, String> params) {
        List<UserDirectoryService.Account> all = userDirectory.findAll();

        String username = params.get("username");
        String name = params.get("name");
        String role = params.get("role");
        String status = params.get("status");
        List<Map<String, Object>> filtered = all.stream()
                .filter(acc -> username == null || acc.loginName().contains(username))
                .filter(acc -> name == null || (acc.name() != null && acc.name().contains(name)))
                .filter(acc -> role == null || role.equals(acc.roleCode()))
                .filter(acc -> status == null || status.equals(acc.enabled() ? "enabled" : "disabled"))
                .map(this::toMap)
                .toList();

        int pageNum = parseInt(params.getOrDefault("pageNum", "1"), 1);
        int pageSize = parseInt(params.getOrDefault("pageSize", "10"), 10);
        int from = Math.max((pageNum - 1) * pageSize, 0);
        int to = Math.min(from + pageSize, filtered.size());

        List<Map<String, Object>> list = from >= filtered.size() ? List.<Map<String, Object>>of() : filtered.subList(from, to);
        return new PageResult(list, filtered.size());
    }

    /**
     * 获取管理员详情
     */
    public AdminUserDTO getAdmin(Long id) {
        return userDirectory.findById(id)
                .map(structMapper::toDto)
                .orElseThrow(() -> new ApiException(404, "管理员不存在"));
    }

    /**
     * 删除管理员
     */
    @Transactional
    public void deleteAdmin(Long id) {
        UserDirectoryService.Account account = userDirectory.findById(id)
                .orElseThrow(() -> new ApiException(404, "管理员不存在"));
        ensureNotLastSuperAdmin(account);
        userDirectory.delete(id);
    }

    private Map<String, Object> toMap(UserDirectoryService.Account account) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", account.id());
        row.put("username", account.loginName());
        row.put("name", account.name() != null ? account.name() : "");
        row.put("avatar", account.avatar());
        row.put("role", account.roleCode() != null ? account.roleCode() : "viewer");
        row.put("status", account.enabled() ? "enabled" : "disabled");
        row.put("createTime", format(account.createTime()));
        row.put("updateTime", format(account.updateTime()));
        row.entrySet().removeIf(entry -> entry.getValue() == null);
        return row;
    }

    private String format(LocalDateTime time) {
        return time == null ? null : DATE_TIME_FORMATTER.format(time);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void ensureNotLastSuperAdmin(UserDirectoryService.Account account) {
        if (!account.enabled() || !isSuperAdminRole(account.roleCode())) {
            return;
        }
        long activeSuperAdmins = userDirectory.findAll().stream()
                .filter(candidate -> !Objects.equals(candidate.id(), account.id()))
                .filter(UserDirectoryService.Account::enabled)
                .filter(candidate -> isSuperAdminRole(candidate.roleCode()))
                .count();
        if (activeSuperAdmins == 0) {
            throw new ApiException(400, "至少保留一个启用的超级管理员");
        }
    }

    private boolean isSuperAdminRole(String roleCode) {
        return "super_admin".equals(roleCode);
    }
}
