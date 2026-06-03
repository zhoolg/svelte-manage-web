package com.zhoolg.manage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.entity.dto.AiGenerateResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiGeneratedFileServiceTest {
    private final AiGeneratedFileService service = new AiGeneratedFileService(new ObjectMapper());

    @Test
    void buildsFlatArchitecturePreviewFilesAndMysqlDraft() {
        List<AiGenerateResponse.GeneratedFile> files = service.buildFiles("repair_order", "报修管理", schema());

        assertThat(files)
                .extracting(AiGenerateResponse.GeneratedFile::path)
                .containsExactly(
                        "server/src/main/java/com/zhoolg/manage/entity/pojo/RepairOrderDO.java",
                        "server/src/main/java/com/zhoolg/manage/mapper/RepairOrderMapper.java",
                        "server/src/main/java/com/zhoolg/manage/service/RepairOrderService.java",
                        "server/src/main/java/com/zhoolg/manage/service/impl/RepairOrderServiceImpl.java",
                        "server/src/main/java/com/zhoolg/manage/controller/admin/RepairOrderController.java",
                        "server/src/main/resources/generated-sql/create_repair_order.sql",
                        "metadata/modules/repair_order.json"
                );
        assertThat(files)
                .filteredOn(file -> file.type().equals("java"))
                .extracting(AiGenerateResponse.GeneratedFile::content)
                .allSatisfy(content -> assertThat(content).doesNotContain("com.zhoolg.manage.modules"));
        assertThat(files)
                .filteredOn(file -> file.type().equals("sql"))
                .singleElement()
                .satisfies(file -> assertThat(file.content())
                        .contains("CREATE TABLE biz_repair_order")
                        .contains("reporter VARCHAR(128) NOT NULL")
                        .contains("issue_type VARCHAR(128)")
                        .contains("description LONGTEXT")
                        .contains("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"));
    }

    private Map<String, Object> schema() {
        return Map.of(
                "module", Map.of("key", "repair_order", "name", "报修管理"),
                "entity", Map.of(
                        "fields", List.of(
                                Map.of("name", "reporter", "type", "String", "formType", "input", "required", true),
                                Map.of("name", "issueType", "type", "String", "formType", "select"),
                                Map.of("name", "description", "type", "String", "formType", "textarea")
                        )
                )
        );
    }
}
