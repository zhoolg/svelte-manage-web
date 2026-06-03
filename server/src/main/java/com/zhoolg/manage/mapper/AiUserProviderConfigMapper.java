package com.zhoolg.manage.mapper;

import com.zhoolg.manage.entity.pojo.AiUserProviderConfigDO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class AiUserProviderConfigMapper {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<AiUserProviderConfigDO> rowMapper = (rs, rowNum) -> {
        AiUserProviderConfigDO entity = new AiUserProviderConfigDO();
        entity.setId(rs.getLong("id"));
        entity.setUserId(rs.getLong("user_id"));
        entity.setProvider(rs.getString("provider"));
        entity.setModel(rs.getString("model"));
        entity.setBaseUrl(rs.getString("base_url"));
        entity.setApiKeyCiphertext(rs.getString("api_key_ciphertext"));
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

    public AiUserProviderConfigMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AiUserProviderConfigDO findByUserId(long userId) {
        List<AiUserProviderConfigDO> rows = jdbcTemplate.query(
                selectSql() + " WHERE user_id = ?",
                rowMapper,
                userId
        );
        return rows.stream().findFirst().orElse(null);
    }

    public void upsert(AiUserProviderConfigDO entity) {
        jdbcTemplate.update(
                "INSERT INTO sys_ai_user_provider_config(user_id, provider, model, base_url, api_key_ciphertext) "
                        + "VALUES (?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE provider = VALUES(provider), model = VALUES(model), "
                        + "base_url = VALUES(base_url), api_key_ciphertext = VALUES(api_key_ciphertext), "
                        + "update_time = CURRENT_TIMESTAMP",
                entity.getUserId(),
                entity.getProvider(),
                entity.getModel(),
                entity.getBaseUrl(),
                entity.getApiKeyCiphertext()
        );
    }

    private String selectSql() {
        return "SELECT id, user_id, provider, model, base_url, api_key_ciphertext, create_time, update_time "
                + "FROM sys_ai_user_provider_config";
    }
}
