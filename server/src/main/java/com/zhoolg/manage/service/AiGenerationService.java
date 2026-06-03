package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.AiApplyPlan;
import com.zhoolg.manage.entity.dto.AiClarificationResponse;
import com.zhoolg.manage.entity.dto.AiGenerateRequest;
import com.zhoolg.manage.entity.dto.AiGenerateResponse;
import com.zhoolg.manage.entity.dto.AiGenerationTaskSummary;
import com.zhoolg.manage.entity.dto.AiPreviewResponse;
import com.zhoolg.manage.entity.dto.AiSmokeTestResult;
import com.zhoolg.manage.entity.dto.AiValidationReport;
import com.zhoolg.manage.entity.pojo.AiGenerationTaskDO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.mapper.AiGenerationTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class AiGenerationService {
    private final AiGenerationTaskMapper mapper;
    private final ObjectMapper objectMapper;
    private final DynamicModuleService dynamicModuleService;
    private final AiModuleSmokeTestService smokeTestService;
    private final AiValidationService validationService;
    private final AiGeneratedFileService generatedFileService;
    private final AiRequirementService requirementService;
    private final AiSchemaService schemaService;
    private final AiMetadataAssembler metadataAssembler;
    private final AiAutoRepairService autoRepairService;

    @Autowired
    public AiGenerationService(
            AiGenerationTaskMapper mapper,
            ObjectMapper objectMapper,
            DynamicModuleService dynamicModuleService,
            AiModuleSmokeTestService smokeTestService,
            AiValidationService validationService,
            AiGeneratedFileService generatedFileService,
            AiRequirementService requirementService,
            AiSchemaService schemaService,
            AiMetadataAssembler metadataAssembler,
            AiAutoRepairService autoRepairService
    ) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.dynamicModuleService = dynamicModuleService;
        this.smokeTestService = smokeTestService;
        this.validationService = validationService;
        this.generatedFileService = generatedFileService;
        this.requirementService = requirementService;
        this.schemaService = schemaService;
        this.metadataAssembler = metadataAssembler;
        this.autoRepairService = autoRepairService;
    }

    public AiGenerationService(
            AiGenerationTaskMapper mapper,
            ObjectMapper objectMapper,
            DynamicModuleService dynamicModuleService,
            AiModuleSmokeTestService smokeTestService,
            AiValidationService validationService,
            AiGeneratedFileService generatedFileService,
            AiRequirementService requirementService,
            AiSchemaService schemaService,
            AiMetadataAssembler metadataAssembler
    ) {
        this(
                mapper,
                objectMapper,
                dynamicModuleService,
                smokeTestService,
                validationService,
                generatedFileService,
                requirementService,
                schemaService,
                metadataAssembler,
                new AiAutoRepairService(metadataAssembler)
        );
    }

    public AiGenerateResponse generate(AiGenerateRequest request) {
        return generate(request, progress -> {
        });
    }

    public AiGenerateResponse generate(AiGenerateRequest request, Consumer<GenerationProgress> progressConsumer) {
        Consumer<GenerationProgress> progress = progressConsumer == null ? item -> {
        } : progressConsumer;
        progress.accept(new GenerationProgress("request", "正在校验生成请求", 8, "running"));
        AiRequirementService.Requirement requirement = requirementService.normalize(request);
        progress.accept(new GenerationProgress("requirement", "业务需求已归一化", 18, "passed"));
        String description = requirement.description();
        String moduleKey = requirement.moduleKey();
        String moduleName = requirement.moduleName();
        String taskNo = "AI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);

        progress.accept(new GenerationProgress("schema", "正在生成模块 schema", 35, "running"));
        Map<String, Object> schema = schemaService.buildSchema(requirement, request);
        progress.accept(new GenerationProgress("schema", "模块 schema 已生成", 62, "passed"));
        progress.accept(new GenerationProgress("files", "正在生成 SQL 和 metadata 文件草稿", 70, "running"));
        List<AiGenerateResponse.GeneratedFile> files = generatedFileService.buildFiles(moduleKey, moduleName, schema);
        progress.accept(new GenerationProgress("files", "SQL 和 metadata 文件草稿已生成", 80, "passed"));

        AiGenerationTaskDO entity = new AiGenerationTaskDO();
        entity.setTaskNo(taskNo);
        entity.setPrompt(description);
        entity.setModuleKey(moduleKey);
        entity.setModuleName(moduleName);
        entity.setStatus("PREVIEW_READY");
        entity.setGeneratedSchema(writeJson(schema));
        entity.setGeneratedFiles(writeJson(files));
        progress.accept(new GenerationProgress("persist", "正在保存生成任务快照", 90, "running"));
        mapper.insert(entity);
        progress.accept(new GenerationProgress("preview", "生成预览已就绪", 100, "passed"));

        return new AiGenerateResponse(taskNo, entity.getStatus(), schema, files);
    }

    public record GenerationProgress(
            String phase,
            String message,
            int progress,
            String status
    ) {
    }

    public AiClarificationResponse clarify(AiGenerateRequest request) {
        return requirementService.clarify(request);
    }

    public List<AiGenerationTaskSummary> recentTasks(int limit) {
        return mapper.selectRecent(limit).stream()
                .map(entity -> new AiGenerationTaskSummary(
                        entity.getTaskNo(),
                        entity.getModuleKey(),
                        entity.getModuleName(),
                        entity.getStatus(),
                        entity.getSmokeTestStatus(),
                        entity.getSmokeTestTime(),
                        entity.getCreateTime(),
                        entity.getUpdateTime()
                ))
                .toList();
    }

    public AiPreviewResponse preview(String taskNo) {
        AiGenerationTaskDO entity = mapper.selectByTaskNo(taskNo);
        if (entity == null) {
            throw new ApiException(404, "AI 生成任务不存在");
        }
        List<AiGenerateResponse.GeneratedFile> files = readJson(
                entity.getGeneratedFiles(),
                new TypeReference<>() {
                }
        );
        Map<String, Object> schema = readJson(entity.getGeneratedSchema(), new TypeReference<>() {
        });
        AiValidationReport validation = validationService.validate(entity, schema, files);
        return new AiPreviewResponse(
                entity.getTaskNo(),
                entity.getStatus(),
                entity.getModuleKey(),
                entity.getModuleName(),
                schema,
                metadataAssembler.previewMetadata(entity, schema, validation),
                files,
                validation
        );
    }

    public AiApplyPlan applyPlan(String taskNo) {
        AiGenerationTaskDO entity = mapper.selectByTaskNo(taskNo);
        if (entity == null) {
            throw new ApiException(404, "AI 生成任务不存在");
        }
        Map<String, Object> schema = readJson(entity.getGeneratedSchema(), new TypeReference<>() {
        });
        List<AiGenerateResponse.GeneratedFile> files = readJson(
                entity.getGeneratedFiles(),
                new TypeReference<>() {
                }
        );
        AiValidationReport validation = validationService.validate(entity, schema, files);
        Map<String, Object> metadata = validation.passed()
                ? metadataAssembler.toFrontendModuleMetadata(entity, schema)
                : Map.of();
        return dynamicModuleService.buildApplyPlan(
                entity.getTaskNo(),
                entity.getModuleKey(),
                entity.getModuleName(),
                metadata,
                schema,
                validation
        );
    }

    public Map<String, Object> apply(String taskNo) {
        AiGenerationTaskDO entity = mapper.selectByTaskNo(taskNo);
        if (entity == null) {
            throw new ApiException(404, "AI 生成任务不存在");
        }
        Map<String, Object> schema = readJson(entity.getGeneratedSchema(), new TypeReference<>() {
        });
        List<AiGenerateResponse.GeneratedFile> files = readJson(
                entity.getGeneratedFiles(),
                new TypeReference<>() {
                }
        );
        AiValidationReport validation = validationService.validate(entity, schema, files);
        if (!validation.passed()) {
            throw new ApiException(400, "AI 生成结果校验未通过");
        }
        Map<String, Object> metadata = metadataAssembler.toFrontendModuleMetadata(entity, schema);
        dynamicModuleService.applyModule(entity.getModuleKey(), entity.getModuleName(), taskNo, metadata, schema);
        AiSmokeTestResult smokeTest = smokeTestService.runAppliedModuleSmokeTest(entity.getModuleKey(), metadata);
        AiAutoRepairService.RepairAttempt repairAttempt = null;
        if (!smokeTest.passed()) {
            repairAttempt = autoRepairService.repair(entity, schema, metadata, smokeTest);
            if (repairAttempt.changed()) {
                schema = repairAttempt.schema();
                metadata = repairAttempt.metadata();
                dynamicModuleService.applyModule(entity.getModuleKey(), entity.getModuleName(), taskNo, metadata, schema);
                smokeTest = smokeTestService.runAppliedModuleSmokeTest(entity.getModuleKey(), metadata);
            }
        }
        mapper.updateSmokeTestResult(taskNo, smokeTest.passed() ? "PASSED" : "FAILED", smokeTestResultJson(smokeTest, repairAttempt));
        mapper.updateStatus(taskNo, "APPLIED_TO_METADATA");
        metadata.put("smokeTest", smokeTest);
        if (repairAttempt != null && repairAttempt.changed()) {
            metadata.put("autoRepair", Map.of(
                    "status", smokeTest.passed() ? "REPAIRED" : "FAILED",
                    "actions", repairAttempt.actions()
            ));
        }
        if (!smokeTest.passed()) {
            throw new ApiException(500, "AI 模块已应用，但自动冒烟测试未通过");
        }
        return metadata;
    }

    public Map<String, Object> rollback(String taskNo) {
        AiGenerationTaskDO entity = mapper.selectByTaskNo(taskNo);
        if (entity == null) {
            throw new ApiException(404, "AI 生成任务不存在");
        }
        dynamicModuleService.rollbackModule(entity.getModuleKey(), taskNo);
        mapper.updateStatus(taskNo, "ROLLED_BACK");
        return Map.of(
                "taskNo", entity.getTaskNo(),
                "moduleKey", entity.getModuleKey(),
                "status", "ROLLED_BACK"
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception ex) {
            throw new ApiException(500, "AI 生成结果序列化失败");
        }
    }

    private String smokeTestResultJson(
            AiSmokeTestResult smokeTest,
            AiAutoRepairService.RepairAttempt repairAttempt
    ) {
        if (repairAttempt == null || !repairAttempt.changed()) {
            return writeJson(smokeTest);
        }
        return writeJson(Map.of(
                "finalSmokeTest", smokeTest,
                "autoRepair", Map.of(
                        "status", smokeTest.passed() ? "REPAIRED" : "FAILED",
                        "actions", repairAttempt.actions()
                )
        ));
    }

    private <T> T readJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception ex) {
            throw new ApiException(500, "AI 生成结果解析失败");
        }
    }
}
