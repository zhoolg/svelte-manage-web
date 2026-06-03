package com.zhoolg.manage.mapper;

import com.zhoolg.manage.entity.pojo.MenuDO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class MenuMapper {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<MenuDO> rowMapper = (rs, rowNum) -> {
        MenuDO entity = new MenuDO();
        entity.setId(rs.getLong("id"));
        entity.setMenuKey(rs.getString("menu_key"));
        entity.setParentKey(rs.getString("parent_key"));
        entity.setLabel(rs.getString("label"));
        entity.setIcon(rs.getString("icon"));
        entity.setPath(rs.getString("path"));
        entity.setModuleId(rs.getString("module_id"));
        entity.setPermissionCode(rs.getString("permission_code"));
        entity.setSortOrder(rs.getInt("sort_order"));
        entity.setHidden(rs.getBoolean("hidden"));
        entity.setEnabled(rs.getBoolean("enabled"));
        entity.setSystemBuiltin(rs.getBoolean("system_builtin"));
        entity.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        Timestamp updateTime = rs.getTimestamp("update_time");
        if (updateTime != null) {
            entity.setUpdateTime(updateTime.toLocalDateTime());
        }
        return entity;
    };

    public MenuMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MenuDO> selectAll() {
        return jdbcTemplate.query(baseSelect() + " ORDER BY parent_key IS NOT NULL, sort_order ASC, id ASC", rowMapper);
    }

    public List<MenuDO> selectEnabled() {
        return jdbcTemplate.query(
                baseSelect() + " WHERE enabled = TRUE ORDER BY parent_key IS NOT NULL, sort_order ASC, id ASC",
                rowMapper
        );
    }

    public MenuDO selectById(Long id) {
        List<MenuDO> rows = jdbcTemplate.query(baseSelect() + " WHERE id = ?", rowMapper, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public MenuDO selectByMenuKey(String menuKey) {
        List<MenuDO> rows = jdbcTemplate.query(baseSelect() + " WHERE menu_key = ?", rowMapper, menuKey);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public MenuDO selectByModuleId(String moduleId) {
        List<MenuDO> rows = jdbcTemplate.query(baseSelect() + " WHERE module_id = ?", rowMapper, moduleId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<MenuDO> selectPage(Map<String, String> params, List<String> searchableFields, int pageNum, int pageSize) {
        StringBuilder sql = new StringBuilder(baseSelect());
        List<Object> args = new ArrayList<>();
        appendWhere(sql, args, params, searchableFields);
        sql.append(" ORDER BY parent_key IS NOT NULL, sort_order ASC, id ASC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add((pageNum - 1) * pageSize);
        return jdbcTemplate.query(sql.toString(), rowMapper, args.toArray());
    }

    public long count(Map<String, String> params, List<String> searchableFields) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_menu");
        List<Object> args = new ArrayList<>();
        appendWhere(sql, args, params, searchableFields);
        Long total = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return total == null ? 0 : total;
    }

    public int insert(MenuDO entity) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updated = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO sys_menu(menu_key, parent_key, label, icon, path, module_id, permission_code, sort_order, hidden, enabled, system_builtin) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            bind(ps, entity);
            return ps;
        }, keyHolder);
        if (keyHolder.getKey() != null) {
            entity.setId(keyHolder.getKey().longValue());
        }
        return updated;
    }

    public int update(MenuDO entity) {
        return jdbcTemplate.update(
                "UPDATE sys_menu SET menu_key = ?, parent_key = ?, label = ?, icon = ?, path = ?, module_id = ?, "
                        + "permission_code = ?, sort_order = ?, hidden = ?, enabled = ?, update_time = CURRENT_TIMESTAMP WHERE id = ?",
                entity.getMenuKey(),
                entity.getParentKey(),
                entity.getLabel(),
                entity.getIcon(),
                entity.getPath(),
                entity.getModuleId(),
                entity.getPermissionCode(),
                entity.getSortOrder(),
                entity.getHidden(),
                entity.getEnabled(),
                entity.getId()
        );
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM sys_menu WHERE id = ?", id);
    }

    public int disableByModuleId(String moduleId) {
        return jdbcTemplate.update(
                "UPDATE sys_menu SET enabled = FALSE, update_time = CURRENT_TIMESTAMP WHERE module_id = ?",
                moduleId
        );
    }

    private String baseSelect() {
        return "SELECT id, menu_key, parent_key, label, icon, path, module_id, permission_code, "
                + "sort_order, hidden, enabled, system_builtin, create_time, update_time FROM sys_menu";
    }

    private void bind(PreparedStatement ps, MenuDO entity) throws java.sql.SQLException {
        ps.setString(1, entity.getMenuKey());
        ps.setString(2, entity.getParentKey());
        ps.setString(3, entity.getLabel());
        ps.setString(4, entity.getIcon());
        ps.setString(5, entity.getPath());
        ps.setString(6, entity.getModuleId());
        ps.setString(7, entity.getPermissionCode());
        ps.setInt(8, entity.getSortOrder() == null ? 0 : entity.getSortOrder());
        ps.setBoolean(9, Boolean.TRUE.equals(entity.getHidden()));
        ps.setBoolean(10, !Boolean.FALSE.equals(entity.getEnabled()));
        ps.setBoolean(11, Boolean.TRUE.equals(entity.getSystemBuiltin()));
    }

    private void appendWhere(
            StringBuilder sql,
            List<Object> args,
            Map<String, String> params,
            List<String> searchableFields
    ) {
        List<String> conditions = new ArrayList<>();
        searchableFields.forEach(field -> {
            String value = params.get(field);
            if (value != null && !value.isBlank()) {
                conditions.add(column(field) + " LIKE ?");
                args.add("%" + value.trim() + "%");
            }
        });
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
    }

    private String column(String field) {
        return switch (field) {
            case "menuKey" -> "menu_key";
            case "parentKey" -> "parent_key";
            case "moduleId" -> "module_id";
            case "permissionCode" -> "permission_code";
            default -> field;
        };
    }
}
