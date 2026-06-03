package com.zhoolg.manage.infrastructure.crud;

import com.zhoolg.manage.entity.dto.AiApplyPlan;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.mapper.AiSchemaMigrationMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicTableServiceTest {

    @Test
    void rejectsMissingRequiredCreateFieldBeforeWritingDatabase() {
        DynamicTableService service = new DynamicTableService(
                mock(JdbcTemplate.class),
                mock(AiSchemaMigrationMapper.class)
        );

        assertThatThrownBy(() -> service.create(resource(), Map.of("status", "pending")))
                .isInstanceOf(ApiException.class)
                .hasMessage("标题不能为空");
    }

    @Test
    void rejectsUnsafeModuleIdentifier() {
        DynamicTableService service = new DynamicTableService(
                mock(JdbcTemplate.class),
                mock(AiSchemaMigrationMapper.class)
        );

        assertThatThrownBy(() -> service.ensureTable("bad-name", Map.of("entity", Map.of("fields", List.of()))))
                .isInstanceOf(ApiException.class)
                .hasMessage("动态模块标识不合法");
    }

    @Test
    void ensureTableRecordsCreateTableMigrationBeforeExecutingDdl() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AiSchemaMigrationMapper migrationMapper = mock(AiSchemaMigrationMapper.class);
        DynamicTableService service = new DynamicTableService(jdbcTemplate, migrationMapper);
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Long.class,
                "biz_repair_order"
        )).thenReturn(0L);
        when(migrationMapper.selectByMigrationKey("repair_order:create_table:biz_repair_order")).thenReturn(null);
        when(migrationMapper.insertApplying(
                org.mockito.ArgumentMatchers.eq("repair_order:create_table:biz_repair_order"),
                org.mockito.ArgumentMatchers.eq("repair_order"),
                org.mockito.ArgumentMatchers.eq("biz_repair_order"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("create_table"),
                org.mockito.ArgumentMatchers.contains("CREATE TABLE IF NOT EXISTS")
        )).thenReturn(true);
        when(jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?",
                String.class,
                "biz_repair_order"
        )).thenReturn(List.of("id", "title", "status", "create_time", "update_time"));

        service.ensureTable("repair_order", schema());

        verify(jdbcTemplate).execute(org.mockito.ArgumentMatchers.contains("CREATE TABLE IF NOT EXISTS `biz_repair_order`"));
        verify(migrationMapper).markApplied("repair_order:create_table:biz_repair_order");
    }

    @Test
    void ensureTableRecordsAddColumnMigration() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AiSchemaMigrationMapper migrationMapper = mock(AiSchemaMigrationMapper.class);
        DynamicTableService service = new DynamicTableService(jdbcTemplate, migrationMapper);
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Long.class,
                "biz_repair_order"
        )).thenReturn(1L);
        when(jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?",
                String.class,
                "biz_repair_order"
        )).thenReturn(List.of("id", "title", "create_time", "update_time"));
        when(migrationMapper.selectByMigrationKey("repair_order:add_column:status")).thenReturn(null);
        when(migrationMapper.insertApplying(
                org.mockito.ArgumentMatchers.eq("repair_order:add_column:status"),
                org.mockito.ArgumentMatchers.eq("repair_order"),
                org.mockito.ArgumentMatchers.eq("biz_repair_order"),
                org.mockito.ArgumentMatchers.eq("status"),
                org.mockito.ArgumentMatchers.eq("add_column"),
                org.mockito.ArgumentMatchers.contains("ALTER TABLE")
        )).thenReturn(true);

        service.ensureTable("repair_order", schema());

        verify(jdbcTemplate).execute(org.mockito.ArgumentMatchers.contains("ADD COLUMN `status`"));
        verify(migrationMapper).markApplied("repair_order:add_column:status");
    }

    @Test
    void tablePlanShowsAddIndexMigrationFromDsl() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DynamicTableService service = new DynamicTableService(
                jdbcTemplate,
                mock(AiSchemaMigrationMapper.class)
        );
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Long.class,
                "biz_repair_order"
        )).thenReturn(1L);
        when(jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?",
                String.class,
                "biz_repair_order"
        )).thenReturn(List.of("id", "title", "status", "create_time", "update_time"));
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                Long.class,
                "biz_repair_order",
                "idx_status"
        )).thenReturn(0L);

        List<AiApplyPlan.Operation> operations = service.tablePlan("repair_order", indexedSchema());

        assertThat(operations)
                .extracting(AiApplyPlan.Operation::action)
                .contains("add_index", "record_migration");
        assertThat(operations.toString()).contains("repair_order:add_index:idx_status");
    }

    @Test
    void ensureTableRecordsAddIndexMigration() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AiSchemaMigrationMapper migrationMapper = mock(AiSchemaMigrationMapper.class);
        DynamicTableService service = new DynamicTableService(jdbcTemplate, migrationMapper);
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Long.class,
                "biz_repair_order"
        )).thenReturn(1L);
        when(jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?",
                String.class,
                "biz_repair_order"
        )).thenReturn(List.of("id", "title", "status", "create_time", "update_time"));
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                Long.class,
                "biz_repair_order",
                "idx_status"
        )).thenReturn(0L);
        when(migrationMapper.selectByMigrationKey("repair_order:add_index:idx_status")).thenReturn(null);
        when(migrationMapper.insertApplying(
                org.mockito.ArgumentMatchers.eq("repair_order:add_index:idx_status"),
                org.mockito.ArgumentMatchers.eq("repair_order"),
                org.mockito.ArgumentMatchers.eq("biz_repair_order"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("add_index"),
                org.mockito.ArgumentMatchers.contains("ADD INDEX")
        )).thenReturn(true);

        service.ensureTable("repair_order", indexedSchema());

        verify(jdbcTemplate).execute(org.mockito.ArgumentMatchers.contains("ADD INDEX `idx_status` (`status`)"));
        verify(migrationMapper).markApplied("repair_order:add_index:idx_status");
    }

    @Test
    void tablePlanBlocksRenameColumnMigration() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DynamicTableService service = new DynamicTableService(
                jdbcTemplate,
                mock(AiSchemaMigrationMapper.class)
        );
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Long.class,
                "biz_repair_order"
        )).thenReturn(1L);
        when(jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?",
                String.class,
                "biz_repair_order"
        )).thenReturn(List.of("id", "title", "status", "create_time", "update_time"));

        List<AiApplyPlan.Operation> operations = service.tablePlan("repair_order", renameSchema());

        assertThat(operations)
                .extracting(AiApplyPlan.Operation::action)
                .contains("blocked_rename_column");
        assertThat(operations)
                .extracting(AiApplyPlan.Operation::riskLevel)
                .contains("high");
    }

    @Test
    void transitionWorkflowUpdatesStatusWhenCurrentStateMatches() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DynamicTableService service = new DynamicTableService(
                jdbcTemplate,
                mock(AiSchemaMigrationMapper.class)
        );
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("id", 1L);
        before.put("status", "submitted");
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("id", 1L);
        after.put("status", "assigned");
        when(jdbcTemplate.queryForList("SELECT * FROM `biz_repair_order` WHERE `id` = ?", 1L))
                .thenReturn(List.of(before), List.of(after));

        service.transitionWorkflow(
                resource(),
                1L,
                Map.of("action", "assign", "from", "submitted", "to", "assigned", "statusField", "status")
        );

        verify(jdbcTemplate).update(
                "UPDATE `biz_repair_order` SET `status` = ? WHERE `id` = ?",
                "assigned",
                1L
        );
    }

    @Test
    void transitionWorkflowRejectsUnexpectedCurrentState() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DynamicTableService service = new DynamicTableService(
                jdbcTemplate,
                mock(AiSchemaMigrationMapper.class)
        );
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 1L);
        row.put("status", "closed");
        when(jdbcTemplate.queryForList("SELECT * FROM `biz_repair_order` WHERE `id` = ?", 1L))
                .thenReturn(List.of(row));

        assertThatThrownBy(() -> service.transitionWorkflow(
                resource(),
                1L,
                Map.of("action", "assign", "from", "submitted", "to", "assigned", "statusField", "status")
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("当前状态不允许执行该工作流动作");
    }

    @Test
    void transitionWorkflowChecksConditionBeforeUpdatingStatus() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DynamicTableService service = new DynamicTableService(
                jdbcTemplate,
                mock(AiSchemaMigrationMapper.class)
        );
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("id", 1L);
        before.put("status", "submitted");
        before.put("priority", 5);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("id", 1L);
        after.put("status", "approved");
        after.put("priority", 5);
        when(jdbcTemplate.queryForList("SELECT * FROM `biz_repair_order` WHERE `id` = ?", 1L))
                .thenReturn(List.of(before), List.of(after));

        service.transitionWorkflow(
                resource(),
                1L,
                Map.of(
                        "action", "approve",
                        "from", "submitted",
                        "to", "approved",
                        "statusField", "status",
                        "condition", Map.of("field", "priority", "operator", "gte", "value", 3)
                )
        );

        verify(jdbcTemplate).update(
                "UPDATE `biz_repair_order` SET `status` = ? WHERE `id` = ?",
                "approved",
                1L
        );
    }

    @Test
    void transitionWorkflowRejectsWhenConditionDoesNotMatch() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DynamicTableService service = new DynamicTableService(
                jdbcTemplate,
                mock(AiSchemaMigrationMapper.class)
        );
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 1L);
        row.put("status", "submitted");
        row.put("priority", 1);
        when(jdbcTemplate.queryForList("SELECT * FROM `biz_repair_order` WHERE `id` = ?", 1L))
                .thenReturn(List.of(row));

        assertThatThrownBy(() -> service.transitionWorkflow(
                resource(),
                1L,
                Map.of(
                        "action", "approve",
                        "from", "submitted",
                        "to", "approved",
                        "statusField", "status",
                        "condition", Map.of("field", "priority", "operator", "gte", "value", 3)
                )
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("当前数据不满足工作流条件");
    }

    private Map<String, Object> schema() {
        return Map.of(
                "entity", Map.of(
                        "fields", List.of(
                                Map.of("name", "title", "label", "标题", "type", "String", "formType", "input", "required", true),
                                Map.of("name", "status", "label", "状态", "type", "String", "formType", "select", "required", true)
                        )
                )
        );
    }

    private Map<String, Object> indexedSchema() {
        return new LinkedHashMap<>(Map.of(
                "entity", Map.of(
                        "fields", List.of(
                                Map.of("name", "title", "label", "标题", "type", "String", "formType", "input", "required", true),
                                Map.of("name", "status", "label", "状态", "type", "String", "formType", "select", "required", true)
                        )
                ),
                "migration", Map.of(
                        "operations", List.of(
                                Map.of(
                                        "type", "add_index",
                                        "name", "idx_status",
                                        "fields", List.of("status"),
                                        "rollback", Map.of("type", "drop_index", "name", "idx_status")
                                )
                        )
                )
        ));
    }

    private Map<String, Object> renameSchema() {
        return new LinkedHashMap<>(Map.of(
                "entity", Map.of(
                        "fields", List.of(
                                Map.of("name", "title", "label", "标题", "type", "String", "formType", "input", "required", true),
                                Map.of("name", "status", "label", "状态", "type", "String", "formType", "select", "required", true)
                        )
                ),
                "migration", Map.of(
                        "operations", List.of(
                                Map.of(
                                        "type", "rename_column",
                                        "from", "oldStatus",
                                        "to", "status",
                                        "rollback", Map.of("type", "rename_column", "from", "status", "to", "oldStatus")
                                )
                        )
                )
        ));
    }

    private ResourceDefinition resource() {
        return new ResourceDefinition(
                "repair_order",
                "/repair_order",
                "报修管理",
                "repair_order",
                "id",
                List.of(),
                List.of(),
                List.of("title", "status"),
                List.of("title", "status"),
                List.of(),
                List.of(),
                List.of(
                        Map.of("field", "title", "label", "标题", "required", true),
                        Map.of("field", "status", "label", "状态", "required", true)
                )
        );
    }
}
