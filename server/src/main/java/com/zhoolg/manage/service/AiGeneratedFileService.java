package com.zhoolg.manage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.entity.dto.AiGenerateResponse;
import com.zhoolg.manage.exception.ApiException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiGeneratedFileService {
    private final ObjectMapper objectMapper;

    public AiGeneratedFileService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<AiGenerateResponse.GeneratedFile> buildFiles(
            String moduleKey,
            String moduleName,
            Map<String, Object> schema
    ) {
        String className = upperCamel(moduleKey);
        String variableName = lowerCamel(className);
        Map<String, Object> entity = objectMap(schema.get("entity"));
        List<Map<String, Object>> fields = objectList(entity.get("fields"));
        return List.of(
                new AiGenerateResponse.GeneratedFile(
                        "server/src/main/java/com/zhoolg/manage/entity/pojo/" + className + "DO.java",
                        "java",
                        "package com.zhoolg.manage.entity.pojo;\n\n"
                                + "import java.math.BigDecimal;\n"
                                + "import java.time.LocalDate;\n"
                                + "import java.time.LocalDateTime;\n\n"
                                + "// AI 草稿：应用前请按业务字段补充校验与字段类型。\n"
                                + "public class " + className + "DO {\n"
                                + javaFields(fields)
                                + "}\n"
                ),
                new AiGenerateResponse.GeneratedFile(
                        "server/src/main/java/com/zhoolg/manage/mapper/" + className + "Mapper.java",
                        "java",
                        "package com.zhoolg.manage.mapper;\n\n"
                                + "import org.springframework.stereotype.Repository;\n\n"
                                + "// AI 草稿：遵循统一 Mapper 层，使用 JdbcTemplate 或项目既有数据访问方式实现。\n"
                                + "@Repository\n"
                                + "public class " + className + "Mapper {\n"
                                + "}\n"
                ),
                new AiGenerateResponse.GeneratedFile(
                        "server/src/main/java/com/zhoolg/manage/service/" + className + "Service.java",
                        "java",
                        "package com.zhoolg.manage.service;\n\n"
                                + "// AI 草稿：服务接口放在 service 层，具体实现放在 service.impl 层。\n"
                                + "public interface " + className + "Service {\n"
                                + "}\n"
                ),
                new AiGenerateResponse.GeneratedFile(
                        "server/src/main/java/com/zhoolg/manage/service/impl/" + className + "ServiceImpl.java",
                        "java",
                        "package com.zhoolg.manage.service.impl;\n\n"
                                + "import com.zhoolg.manage.service." + className + "Service;\n"
                                + "import org.springframework.stereotype.Service;\n\n"
                                + "// AI 草稿：实现类统一收敛在 service.impl 包。\n"
                                + "@Service\n"
                                + "public class " + className + "ServiceImpl implements " + className + "Service {\n"
                                + "}\n"
                ),
                new AiGenerateResponse.GeneratedFile(
                        "server/src/main/java/com/zhoolg/manage/controller/admin/" + className + "Controller.java",
                        "java",
                        "package com.zhoolg.manage.controller.admin;\n\n"
                                + "import com.zhoolg.manage.service." + className + "Service;\n"
                                + "import org.springframework.web.bind.annotation.RequestMapping;\n"
                                + "import org.springframework.web.bind.annotation.RestController;\n\n"
                                + "// AI 草稿：控制器继续放在 controller.admin，保持当前后台路由分层。\n"
                                + "@RestController\n"
                                + "@RequestMapping(\"/admin/" + moduleKey.replace("_", "-") + "\")\n"
                                + "public class " + className + "Controller {\n"
                                + "    private final " + className + "Service " + variableName + "Service;\n\n"
                                + "    public " + className + "Controller(" + className + "Service " + variableName + "Service) {\n"
                                + "        this." + variableName + "Service = " + variableName + "Service;\n"
                                + "    }\n"
                                + "}\n"
                        ),
                new AiGenerateResponse.GeneratedFile(
                        "server/src/main/resources/generated-sql/create_" + moduleKey + ".sql",
                        "sql",
                        mysqlCreateTable(moduleKey, fields)
                ),
                new AiGenerateResponse.GeneratedFile(
                        "metadata/modules/" + moduleKey + ".json",
                        "metadata",
                        writeJson(schema)
                )
        );
    }

    private String upperCamel(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.split("_")) {
            if (part.isBlank()) {
                continue;
            }
            result.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                result.append(part.substring(1));
            }
        }
        return result.toString();
    }

    private String lowerCamel(String value) {
        if (value == null || value.isBlank()) {
            return "module";
        }
        return value.substring(0, 1).toLowerCase(Locale.ROOT) + value.substring(1);
    }

    private String javaFields(List<Map<String, Object>> fields) {
        StringBuilder builder = new StringBuilder("    private Long id;\n");
        for (Map<String, Object> field : fields) {
            builder.append("    private ")
                    .append(field.get("type"))
                    .append(' ')
                    .append(field.get("name"))
                    .append(";\n");
        }
        return builder.toString();
    }

    private String mysqlCreateTable(String moduleKey, List<Map<String, Object>> fields) {
        StringBuilder builder = new StringBuilder("CREATE TABLE biz_")
                .append(moduleKey)
                .append(" (\n  id BIGINT PRIMARY KEY AUTO_INCREMENT");
        for (Map<String, Object> field : fields) {
            builder.append(",\n  ")
                    .append(snakeCase(String.valueOf(field.get("name"))))
                    .append(' ')
                    .append(mysqlType(String.valueOf(field.get("type")), String.valueOf(field.get("formType"))));
            if (Boolean.TRUE.equals(field.get("required"))) {
                builder.append(" NOT NULL");
            }
            if ("createTime".equals(field.get("name"))) {
                builder.append(" DEFAULT CURRENT_TIMESTAMP");
            }
        }
        builder.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;\n");
        return builder.toString();
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

    private String snakeCase(String value) {
        return value.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return Map.of();
    }

    private List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> map = objectMap(item);
            if (!map.isEmpty()) {
                result.add(map);
            }
        }
        return result;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception ex) {
            throw new ApiException(500, "AI 生成结果序列化失败");
        }
    }
}
