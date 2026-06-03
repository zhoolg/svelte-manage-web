package com.zhoolg.manage.mapper;

import com.zhoolg.manage.entity.pojo.AdminUserDO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * 管理员账号 Mapper：统一使用 Spring JDBC，避免后端同时维护 JPA 与 JDBC 两套持久化模型。
 */
@Repository
public class AdminUserMapper {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<AdminUserDO> rowMapper = (rs, rowNum) -> {
        AdminUserDO entity = new AdminUserDO();
        entity.setId(rs.getLong("id"));
        entity.setUsername(rs.getString("username"));
        entity.setPasswordHash(rs.getString("password_hash"));
        entity.setName(rs.getString("name"));
        entity.setAvatar(rs.getString("avatar"));
        entity.setRoleCode(rs.getString("role_code"));
        entity.setEnabled(rs.getBoolean("enabled"));
        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            entity.setCreateTime(createTime.toLocalDateTime());
        }
        Timestamp updateTime = rs.getTimestamp("update_time");
        if (updateTime != null) {
            entity.setUpdateTime(updateTime.toLocalDateTime());
        }
        return entity;
    };

    public AdminUserMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AdminUserDO> findByUsername(String username) {
        List<AdminUserDO> rows = jdbcTemplate.query(
                selectSql() + " WHERE username = ?",
                rowMapper,
                username
        );
        return rows.stream().findFirst();
    }

    public Optional<AdminUserDO> findById(Long id) {
        List<AdminUserDO> rows = jdbcTemplate.query(
                selectSql() + " WHERE id = ?",
                rowMapper,
                id
        );
        return rows.stream().findFirst();
    }

    public List<AdminUserDO> findAll() {
        return jdbcTemplate.query(selectSql() + " ORDER BY id ASC", rowMapper);
    }

    public boolean existsByUsername(String username) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_admin_user WHERE username = ?",
                Long.class,
                username
        );
        return total != null && total > 0;
    }

    public boolean existsById(Long id) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_admin_user WHERE id = ?",
                Long.class,
                id
        );
        return total != null && total > 0;
    }

    public AdminUserDO save(AdminUserDO entity) {
        if (entity.getId() == null) {
            insert(entity);
        } else {
            update(entity);
        }
        return findById(entity.getId()).orElse(entity);
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM sys_admin_user WHERE id = ?", id);
    }

    private void insert(AdminUserDO entity) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO sys_admin_user(username, password_hash, name, avatar, role_code, enabled) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, entity.getUsername());
            ps.setString(2, entity.getPasswordHash());
            ps.setString(3, entity.getName());
            ps.setString(4, entity.getAvatar());
            ps.setString(5, entity.getRoleCode());
            ps.setBoolean(6, Boolean.TRUE.equals(entity.getEnabled()));
            return ps;
        }, keyHolder);
        if (keyHolder.getKey() != null) {
            entity.setId(keyHolder.getKey().longValue());
        }
    }

    private void update(AdminUserDO entity) {
        jdbcTemplate.update(
                "UPDATE sys_admin_user SET username = ?, password_hash = ?, name = ?, avatar = ?, role_code = ?, "
                        + "enabled = ?, update_time = CURRENT_TIMESTAMP WHERE id = ?",
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getName(),
                entity.getAvatar(),
                entity.getRoleCode(),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getId()
        );
    }

    private String selectSql() {
        return "SELECT id, username, password_hash, name, avatar, role_code, enabled, create_time, update_time FROM sys_admin_user";
    }
}
