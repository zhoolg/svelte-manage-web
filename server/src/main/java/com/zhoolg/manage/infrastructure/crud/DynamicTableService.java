package com.zhoolg.manage.infrastructure.crud;

import com.zhoolg.manage.entity.base.PageResult;
import com.zhoolg.manage.entity.dto.AiApplyPlan;
import com.zhoolg.manage.entity.pojo.AiSchemaMigrationDO;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.mapper.AiSchemaMigrationMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DynamicTableService {
    private final JdbcTemplate jdbcTemplate;
    private final AiSchemaMigrationMapper migrationMapper;

    public DynamicTableService(JdbcTemplate jdbcTemplate, AiSchemaMigrationMapper migrationMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.migrationMapper = migrationMapper;
    }

    public void ensureTable(String moduleKey, Map<String, Object> schema) {
        String tableName = tableName(moduleKey);
        List<ColumnDefinition> businessColumns = businessColumns(schema);

        List<String> columns = new ArrayList<>();
        columns.add("`id` BIGINT PRIMARY KEY AUTO_INCREMENT");
        businessColumns.forEach(column -> columns.add(column.createSql()));
        String createTableSql = "CREATE TABLE IF NOT EXISTS " + quote(tableName) + " (\n  "
                + String.join(",\n  ", columns)
                + "\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        if (!tableExists(tableName)) {
            applyMigration(
                    migrationKey(moduleKey, "create_table", tableName),
                    moduleKey,
                    tableName,
                    null,
                    "create_table",
                    createTableSql,
                    () -> jdbcTemplate.execute(createTableSql)
            );
        }

        Set<String> existingColumns = existingColumns(tableName);
        for (ColumnDefinition column : businessColumns) {
            if (!existingColumns.contains(column.name())) {
                String ddlSql = "ALTER TABLE " + quote(tableName) + " ADD COLUMN " + column.alterSql();
                applyMigration(
                        migrationKey(moduleKey, "add_column", column.name()),
                        moduleKey,
                        tableName,
                        column.name(),
                        "add_column",
                        ddlSql,
                        () -> jdbcTemplate.execute(ddlSql)
                );
            }
        }
        applyDslMigrations(moduleKey, tableName, schema);
    }

    public List<AiApplyPlan.Operation> tablePlan(String moduleKey, Map<String, Object> schema) {
        String tableName = tableName(moduleKey);
        List<ColumnDefinition> businessColumns = businessColumns(schema);
        List<AiApplyPlan.Operation> operations = new ArrayList<>();
        boolean exists = tableExists(tableName);
        if (!exists) {
            operations.add(operation(
                    "database",
                    "create_table",
                    tableName,
                    "创建动态业务表 " + tableName + "，包含 id、业务字段、create_time、update_time",
                    "medium"
            ));
            operations.add(operation(
                    "migration",
                    "record_migration",
                    migrationKey(moduleKey, "create_table", tableName),
                    "记录并幂等执行建表迁移，状态写入 sys_ai_schema_migration",
                    "low"
            ));
        }

        Set<String> availableColumns = businessColumnNames(businessColumns);
        if (exists) {
            Set<String> existingColumns = existingColumns(tableName);
            availableColumns = new LinkedHashSet<>(existingColumns);
            availableColumns.addAll(businessColumnNames(businessColumns));
            for (ColumnDefinition column : businessColumns) {
                if (!existingColumns.contains(column.name())) {
                    operations.add(operation(
                            "database",
                            "add_column",
                            tableName + "." + column.name(),
                            "向动态业务表追加字段 `" + column.name() + "` " + column.alterDefinition(),
                            "medium"
                    ));
                    operations.add(operation(
                            "migration",
                            "record_migration",
                            migrationKey(moduleKey, "add_column", column.name()),
                            "记录并幂等执行追加字段迁移，状态写入 sys_ai_schema_migration",
                            "low"
                    ));
                }
            }
        }
        operations.addAll(migrationPlan(moduleKey, tableName, schema, availableColumns, exists));
        if (operations.isEmpty()) {
            operations.add(operation(
                    "database",
                    "no_change",
                    tableName,
                    "动态业务表已具备本次 schema 需要的字段",
                    "low"
            ));
        }
        return operations;
    }

    public PageResult page(ResourceDefinition resource, Map<String, String> params) {
        String tableName = tableName(resource.key());
        List<String> fields = selectFields(resource);
        int pageNum = parseInt(params.getOrDefault("pageNum", params.getOrDefault("page", "1")), 1);
        int pageSize = parseInt(params.getOrDefault("pageSize", params.getOrDefault("size", "10")), 10);
        List<Object> args = new ArrayList<>();
        String where = whereClause(params, resource, args);
        String sql = "SELECT " + selectList(fields) + " FROM " + quote(tableName) + where
                + " ORDER BY `id` DESC LIMIT ? OFFSET ?";
        args.add(pageSize);
        args.add((pageNum - 1) * pageSize);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args.toArray());

        List<Object> countArgs = new ArrayList<>();
        String countWhere = whereClause(params, resource, countArgs);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + quote(tableName) + countWhere,
                Long.class,
                countArgs.toArray()
        );
        return new PageResult(rows, total == null ? 0L : total);
    }

    public Map<String, Object> create(ResourceDefinition resource, Map<String, Object> payload) {
        String tableName = tableName(resource.key());
        if (payload.isEmpty()) {
            throw new ApiException(400, "新增数据不能为空");
        }
        validateRequiredFields(resource, payload, true);
        List<String> fields = payload.keySet().stream()
                .filter(field -> !"id".equals(field))
                .toList();
        if (fields.isEmpty()) {
            throw new ApiException(400, "新增数据不能为空");
        }
        String columns = fields.stream().map(this::columnName).map(this::quote).reduce((a, b) -> a + ", " + b).orElse("");
        String placeholders = String.join(", ", java.util.Collections.nCopies(fields.size(), "?"));
        Object[] args = fields.stream().map(payload::get).toArray();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO " + quote(tableName) + " (" + columns + ") VALUES (" + placeholders + ")",
                    Statement.RETURN_GENERATED_KEYS
            );
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keyHolder);
        Object id = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
        return selectById(resource.key(), id);
    }

    public Map<String, Object> update(ResourceDefinition resource, Object id, Map<String, Object> payload) {
        String tableName = tableName(resource.key());
        Long parsedId = parseId(id);
        validateRequiredFields(resource, payload, false);
        List<String> fields = payload.keySet().stream()
                .filter(field -> !"id".equals(field))
                .toList();
        if (fields.isEmpty()) {
            return selectById(resource.key(), parsedId);
        }
        String setClause = fields.stream()
                .map(field -> quote(columnName(field)) + " = ?")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        List<Object> args = new ArrayList<>(fields.stream().map(payload::get).toList());
        args.add(parsedId);
        jdbcTemplate.update("UPDATE " + quote(tableName) + " SET " + setClause + " WHERE `id` = ?", args.toArray());
        return selectById(resource.key(), parsedId);
    }

    public Map<String, Object> transitionWorkflow(
            ResourceDefinition resource,
            Object id,
            Map<String, Object> transition
    ) {
        String statusField = text(transition.getOrDefault("statusField", "status"));
        String from = text(transition.get("from"));
        String to = text(transition.get("to"));
        if (statusField.isBlank() || from.isBlank() || to.isBlank()) {
            throw new ApiException(400, "工作流定义不完整");
        }
        Map<String, Object> current = selectById(resource.key(), id);
        Object currentStatus = current.get(statusField);
        if (!from.equals(String.valueOf(currentStatus))) {
            throw new ApiException(400, "当前状态不允许执行该工作流动作");
        }
        if (!workflowConditionPassed(current, transition.get("condition"))) {
            throw new ApiException(400, "当前数据不满足工作流条件");
        }
        return update(resource, id, Map.of(statusField, to));
    }

    public void delete(ResourceDefinition resource, Object id) {
        jdbcTemplate.update("DELETE FROM " + quote(tableName(resource.key())) + " WHERE `id` = ?", parseId(id));
    }

    private Map<String, Object> selectById(String key, Object id) {
        Long parsedId = parseId(id);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + quote(tableName(key)) + " WHERE `id` = ?",
                parsedId
        );
        if (rows.isEmpty()) {
            throw new ApiException(404, "动态模块数据不存在");
        }
        return toCamelRow(rows.get(0));
    }

    private String whereClause(Map<String, String> params, ResourceDefinition resource, List<Object> args) {
        List<String> conditions = new ArrayList<>();
        for (String field : resource.searchableFields()) {
            String value = params.get(field);
            if (value != null && !value.isBlank()) {
                conditions.add(quote(columnName(field)) + " LIKE ?");
                args.add("%" + value.trim() + "%");
            }
        }
        for (String field : resource.filterFields()) {
            String value = params.get(field);
            if (value != null && !value.isBlank()) {
                conditions.add(quote(columnName(field)) + " = ?");
                args.add(value.trim());
            }
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private List<String> selectFields(ResourceDefinition resource) {
        Set<String> fields = new LinkedHashSet<>();
        fields.add("id");
        for (Map<String, Object> column : resource.columns()) {
            String field = text(column.get("field"));
            if (!field.isBlank()) {
                fields.add(field);
            }
        }
        fields.add("createTime");
        fields.add("updateTime");
        return List.copyOf(fields);
    }

    private String selectList(List<String> fields) {
        return fields.stream()
                .map(field -> quote(columnName(field)) + " AS `" + field + "`")
                .reduce((a, b) -> a + ", " + b)
                .orElse("`id`");
    }

    private Map<String, Object> toCamelRow(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        row.forEach((key, value) -> result.put(camelCase(key), value));
        return result;
    }

    private String tableName(String moduleKey) {
        String key = requireIdentifier(moduleKey, "动态模块标识不合法");
        return "biz_" + key;
    }

    private String columnName(String field) {
        String snake = field.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
        return requireIdentifier(snake, "动态模块字段不合法");
    }

    private String requireIdentifier(String value, String message) {
        if (value == null || !value.matches("[a-z][a-z0-9_]{0,63}")) {
            throw new ApiException(400, message);
        }
        return value;
    }

    private String quote(String identifier) {
        return "`" + requireIdentifier(identifier, "SQL 标识符不合法") + "`";
    }

    private Long parseId(Object id) {
        try {
            return Long.valueOf(String.valueOf(id));
        } catch (NumberFormatException ex) {
            throw new ApiException(400, "无效 ID");
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Math.min(Math.max(Integer.parseInt(value), 1), 100);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String mysqlType(String type, String formType) {
        return switch (type) {
            case "Integer" -> "INT";
            case "Long" -> "BIGINT";
            case "BigDecimal" -> "DECIMAL(12,2)";
            case "LocalDate" -> "DATE";
            case "LocalDateTime" -> "DATETIME";
            default -> "textarea".equals(formType) ? "LONGTEXT" : "VARCHAR(128)";
        };
    }

    private String camelCase(String value) {
        String[] parts = value.split("_");
        StringBuilder result = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isBlank()) {
                result.append(parts[i].substring(0, 1).toUpperCase(java.util.Locale.ROOT));
                if (parts[i].length() > 1) {
                    result.append(parts[i].substring(1));
                }
            }
        }
        return result.toString();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private void validateRequiredFields(ResourceDefinition resource, Map<String, Object> payload, boolean create) {
        for (Map<String, Object> field : resource.form()) {
            String fieldName = text(field.get("field"));
            if (fieldName.isBlank() || !Boolean.TRUE.equals(field.get("required"))) {
                continue;
            }
            if (!create && !payload.containsKey(fieldName)) {
                continue;
            }
            Object value = payload.get(fieldName);
            if (value == null || String.valueOf(value).trim().isBlank()) {
                String label = text(field.get("label"));
                throw new ApiException(400, (label.isBlank() ? fieldName : label) + "不能为空");
            }
        }
    }

    private Set<String> existingColumns(String tableName) {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?",
                String.class,
                tableName
        );
        return columns.stream()
                .map(column -> column.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
    }

    private Set<String> businessColumnNames(List<ColumnDefinition> businessColumns) {
        Set<String> columnNames = new LinkedHashSet<>(Set.of("id"));
        businessColumns.forEach(column -> columnNames.add(column.name()));
        return columnNames;
    }

    private boolean indexExists(String tableName, String indexName) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                Long.class,
                tableName,
                indexName
        );
        return count != null && count > 0;
    }

    private void applyDslMigrations(String moduleKey, String tableName, Map<String, Object> schema) {
        Set<String> existingColumns = existingColumns(tableName);
        for (MigrationOperation operation : migrationOperations(schema)) {
            switch (operation.type()) {
                case "add_index" -> applyAddIndex(moduleKey, tableName, existingColumns, operation);
                case "drop_index" -> applyDropIndex(moduleKey, tableName, operation);
                case "rename_column", "drop_column", "change_column" -> throw new ApiException(
                        400,
                        "迁移 DSL 包含高风险结构变更，已阻断自动执行：" + operation.type()
                );
                default -> throw new ApiException(400, "不支持的迁移 DSL 操作：" + operation.type());
            }
        }
    }

    private void applyAddIndex(
            String moduleKey,
            String tableName,
            Set<String> existingColumns,
            MigrationOperation operation
    ) {
        String indexName = requireIdentifier(operation.name(), "索引名称不合法");
        List<String> fields = indexFields(operation, existingColumns);
        if (indexExists(tableName, indexName)) {
            return;
        }
        String ddlSql = "ALTER TABLE " + quote(tableName)
                + " ADD INDEX " + quote(indexName)
                + " (" + fields.stream().map(this::quote).reduce((a, b) -> a + ", " + b).orElse("") + ")";
        applyMigration(
                migrationKey(moduleKey, "add_index", indexName),
                moduleKey,
                tableName,
                null,
                "add_index",
                ddlSql,
                () -> jdbcTemplate.execute(ddlSql)
        );
    }

    private void applyDropIndex(String moduleKey, String tableName, MigrationOperation operation) {
        String indexName = requireIdentifier(operation.name(), "索引名称不合法");
        if (!indexExists(tableName, indexName)) {
            return;
        }
        String ddlSql = "ALTER TABLE " + quote(tableName) + " DROP INDEX " + quote(indexName);
        applyMigration(
                migrationKey(moduleKey, "drop_index", indexName),
                moduleKey,
                tableName,
                null,
                "drop_index",
                ddlSql,
                () -> jdbcTemplate.execute(ddlSql)
        );
    }

    private List<AiApplyPlan.Operation> migrationPlan(
            String moduleKey,
            String tableName,
            Map<String, Object> schema,
            Set<String> availableColumns,
            boolean tableExists
    ) {
        List<AiApplyPlan.Operation> operations = new ArrayList<>();
        for (MigrationOperation migration : migrationOperations(schema)) {
            switch (migration.type()) {
                case "add_index" -> {
                    String indexName = requireIdentifier(migration.name(), "索引名称不合法");
                    List<String> fields = indexFields(migration, availableColumns);
                    if (!tableExists || !indexExists(tableName, indexName)) {
                        operations.add(operation(
                                "database",
                                "add_index",
                                tableName + "." + indexName,
                                "为动态业务表追加索引 `" + indexName + "`，字段："
                                        + String.join(", ", fields) + rollbackDescription(migration),
                                "low"
                        ));
                        operations.add(operation(
                                "migration",
                                "record_migration",
                                migrationKey(moduleKey, "add_index", indexName),
                                "记录并幂等执行索引新增迁移，状态写入 sys_ai_schema_migration",
                                "low"
                        ));
                    }
                }
                case "drop_index" -> {
                    String indexName = requireIdentifier(migration.name(), "索引名称不合法");
                    if (tableExists && indexExists(tableName, indexName)) {
                        operations.add(operation(
                                "database",
                                "drop_index",
                                tableName + "." + indexName,
                                "移除动态业务表索引 `" + indexName + "`" + rollbackDescription(migration),
                                "medium"
                        ));
                        operations.add(operation(
                                "migration",
                                "record_migration",
                                migrationKey(moduleKey, "drop_index", indexName),
                                "记录并幂等执行索引移除迁移，状态写入 sys_ai_schema_migration",
                                "low"
                        ));
                    }
                }
                case "rename_column" -> operations.add(operation(
                        "migration",
                        "blocked_rename_column",
                        tableName + "." + migration.from() + "->" + migration.to(),
                        "字段重命名需要数据兼容窗口和回滚确认，当前自动应用已阻断" + rollbackDescription(migration),
                        "high"
                ));
                case "drop_column", "change_column" -> operations.add(operation(
                        "migration",
                        "blocked_" + migration.type(),
                        tableName,
                        "高风险结构变更需要人工确认迁移窗口，当前自动应用已阻断" + rollbackDescription(migration),
                        "high"
                ));
                default -> operations.add(operation(
                        "migration",
                        "unsupported",
                        migration.type(),
                        "迁移 DSL 操作不在白名单内，当前自动应用已阻断",
                        "high"
                ));
            }
        }
        return operations;
    }

    @SuppressWarnings("unchecked")
    private List<MigrationOperation> migrationOperations(Map<String, Object> schema) {
        Object migrationObject = schema.get("migration");
        if (!(migrationObject instanceof Map<?, ?> migration)) {
            return List.of();
        }
        Object operationsObject = migration.get("operations");
        if (!(operationsObject instanceof List<?> list)) {
            return List.of();
        }
        List<MigrationOperation> operations = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            String type = text(map.get("type")).toLowerCase(Locale.ROOT);
            if (type.isBlank()) {
                continue;
            }
            List<String> fields = new ArrayList<>();
            Object fieldsObject = map.get("fields");
            if (fieldsObject instanceof List<?> fieldList) {
                for (Object field : fieldList) {
                    String name = text(field);
                    if (!name.isBlank()) {
                        fields.add(columnName(name));
                    }
                }
            }
            operations.add(new MigrationOperation(
                    type,
                    text(map.get("name")),
                    fields,
                    columnNameOrBlank(map.get("from")),
                    columnNameOrBlank(map.get("to")),
                    map.get("rollback")
            ));
        }
        return operations;
    }

    private String columnNameOrBlank(Object value) {
        String text = text(value);
        return text.isBlank() ? "" : columnName(text);
    }

    private List<String> indexFields(MigrationOperation operation, Set<String> existingColumns) {
        if (operation.fields().isEmpty()) {
            throw new ApiException(400, "索引迁移必须包含 fields");
        }
        for (String field : operation.fields()) {
            if (!existingColumns.contains(field)) {
                throw new ApiException(400, "索引字段不存在：" + field);
            }
        }
        return operation.fields();
    }

    private String rollbackDescription(MigrationOperation operation) {
        if (operation.rollback() == null) {
            return "";
        }
        return "；已声明回滚策略";
    }

    private boolean workflowConditionPassed(Map<String, Object> current, Object conditionObject) {
        if (!(conditionObject instanceof Map<?, ?> condition) || condition.isEmpty()) {
            return true;
        }
        String field = text(condition.get("field"));
        String operator = text(condition.containsKey("operator") ? condition.get("operator") : "eq");
        if (field.isBlank() || operator.isBlank()) {
            throw new ApiException(400, "工作流条件定义不完整");
        }
        Object actual = current.get(field);
        Object expected = condition.get("value");
        return switch (operator) {
            case "eq" -> String.valueOf(actual).equals(String.valueOf(expected));
            case "ne" -> !String.valueOf(actual).equals(String.valueOf(expected));
            case "gt" -> compare(actual, expected) > 0;
            case "gte" -> compare(actual, expected) >= 0;
            case "lt" -> compare(actual, expected) < 0;
            case "lte" -> compare(actual, expected) <= 0;
            case "notBlank" -> actual != null && !String.valueOf(actual).trim().isBlank();
            default -> throw new ApiException(400, "不支持的工作流条件操作符：" + operator);
        };
    }

    private int compare(Object actual, Object expected) {
        if (actual == null || expected == null) {
            throw new ApiException(400, "工作流条件比较值不能为空");
        }
        try {
            return new BigDecimal(String.valueOf(actual)).compareTo(new BigDecimal(String.valueOf(expected)));
        } catch (NumberFormatException ex) {
            return String.valueOf(actual).compareTo(String.valueOf(expected));
        }
    }

    @SuppressWarnings("unchecked")
    private List<ColumnDefinition> businessColumns(Map<String, Object> schema) {
        Object entityObject = schema.get("entity");
        if (!(entityObject instanceof Map<?, ?> entity)) {
            throw new ApiException(400, "AI schema 缺少实体定义，无法创建结构化表");
        }
        Object fieldsObject = entity.get("fields");
        if (!(fieldsObject instanceof List<?> fields) || fields.isEmpty()) {
            throw new ApiException(400, "AI schema 缺少字段定义，无法创建结构化表");
        }

        List<ColumnDefinition> businessColumns = new ArrayList<>();
        Set<String> columnNames = new LinkedHashSet<>(Set.of("id"));
        for (Object item : fields) {
            if (!(item instanceof Map<?, ?> field)) {
                continue;
            }
            String name = text(field.get("name"));
            if (name.isBlank() || "id".equals(name)) {
                continue;
            }
            String column = columnName(name);
            if (!columnNames.add(column)) {
                continue;
            }
            String type = text(field.get("type"));
            String formType = text(field.get("formType"));
            String mysqlType = mysqlType(type, formType);
            StringBuilder createDefinition = new StringBuilder(mysqlType);
            StringBuilder alterDefinition = new StringBuilder(mysqlType);
            if (Boolean.TRUE.equals(field.get("required")) && !"createTime".equals(name) && !"updateTime".equals(name)) {
                createDefinition.append(" NOT NULL");
            }
            if ("createTime".equals(name)) {
                createDefinition.append(" DEFAULT CURRENT_TIMESTAMP");
                alterDefinition.append(" DEFAULT CURRENT_TIMESTAMP");
            }
            if ("updateTime".equals(name)) {
                createDefinition.append(" DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
                alterDefinition.append(" DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
            }
            businessColumns.add(new ColumnDefinition(column, createDefinition.toString(), alterDefinition.toString()));
        }
        if (!columnNames.contains("create_time")) {
            businessColumns.add(new ColumnDefinition(
                    "create_time",
                    "DATETIME DEFAULT CURRENT_TIMESTAMP",
                    "DATETIME DEFAULT CURRENT_TIMESTAMP"
            ));
        }
        if (!columnNames.contains("update_time")) {
            businessColumns.add(new ColumnDefinition(
                    "update_time",
                    "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP",
                    "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
            ));
        }
        return businessColumns;
    }

    private boolean tableExists(String tableName) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Long.class,
                tableName
        );
        return count != null && count > 0;
    }

    private void applyMigration(
            String migrationKey,
            String moduleKey,
            String tableName,
            String columnName,
            String operationType,
            String ddlSql,
            Runnable ddlExecutor
    ) {
        AiSchemaMigrationDO existing = migrationMapper.selectByMigrationKey(migrationKey);
        if (existing != null && "APPLIED".equals(existing.getStatus())) {
            return;
        }
        if (existing == null) {
            boolean inserted = migrationMapper.insertApplying(
                    migrationKey,
                    moduleKey,
                    tableName,
                    columnName,
                    operationType,
                    ddlSql
            );
            if (!inserted) {
                throw new ApiException(409, "动态表迁移正在执行，请稍后重试");
            }
        } else if (migrationMapper.markApplying(migrationKey) == 0) {
            return;
        }

        try {
            ddlExecutor.run();
            migrationMapper.markApplied(migrationKey);
        } catch (RuntimeException ex) {
            migrationMapper.markFailed(migrationKey, ex.getMessage());
            throw ex;
        }
    }

    private String migrationKey(String moduleKey, String operationType, String target) {
        return moduleKey + ":" + operationType + ":" + target;
    }

    private AiApplyPlan.Operation operation(
            String category,
            String action,
            String target,
            String description,
            String riskLevel
    ) {
        return new AiApplyPlan.Operation(category, action, target, description, riskLevel);
    }

    private record ColumnDefinition(String name, String createDefinition, String alterDefinition) {
        private String createSql() {
            return "`" + name + "` " + createDefinition;
        }

        private String alterSql() {
            return "`" + name + "` " + alterDefinition;
        }
    }

    private record MigrationOperation(
            String type,
            String name,
            List<String> fields,
            String from,
            String to,
            Object rollback
    ) {
    }
}
