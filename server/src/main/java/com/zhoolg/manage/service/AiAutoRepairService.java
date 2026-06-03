package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.AiSmokeTestResult;
import com.zhoolg.manage.entity.pojo.AiGenerationTaskDO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiAutoRepairService {
    private final AiMetadataAssembler metadataAssembler;

    public AiAutoRepairService(AiMetadataAssembler metadataAssembler) {
        this.metadataAssembler = metadataAssembler;
    }

    public RepairAttempt repair(
            AiGenerationTaskDO entity,
            Map<String, Object> schema,
            Map<String, Object> metadata,
            AiSmokeTestResult failedSmokeTest
    ) {
        List<String> actions = new ArrayList<>();
        Map<String, Object> repairedSchema = copyMap(schema);
        Map<String, Object> repairedMetadata = copyMap(metadata);
        String moduleKey = entity.getModuleKey();

        if (hasDiagnostic(failedSmokeTest, "resource_definition")) {
            repairedMetadata = metadataAssembler.toFrontendModuleMetadata(entity, repairedSchema);
            actions.add("metadata.rebuild_from_schema");
        }

        if (hasDiagnostic(failedSmokeTest, "permission_catalog") || hasDiagnostic(failedSmokeTest, "role_binding")) {
            ensurePermissions(repairedSchema, repairedMetadata, moduleKey, actions);
        }

        if (hasDiagnostic(failedSmokeTest, "menu")) {
            ensureMenuMetadata(repairedMetadata, entity, actions);
        }

        ensureCrudDefaults(repairedMetadata, moduleKey, entity.getModuleName(), repairedSchema, actions);

        if (!actions.isEmpty()) {
            Map<String, Object> repairInfo = new LinkedHashMap<>();
            repairInfo.put("status", "ATTEMPTED");
            repairInfo.put("source", "deterministic-smoke-diagnostics");
            repairInfo.put("actions", actions);
            repairedMetadata.put("autoRepair", repairInfo);
        }
        return new RepairAttempt(!actions.isEmpty(), repairedSchema, repairedMetadata, actions);
    }

    private boolean hasDiagnostic(AiSmokeTestResult smokeTest, String code) {
        return smokeTest.diagnostics() != null
                && smokeTest.diagnostics().stream().anyMatch(diagnostic -> code.equals(diagnostic.code()));
    }

    @SuppressWarnings("unchecked")
    private void ensurePermissions(
            Map<String, Object> schema,
            Map<String, Object> metadata,
            String moduleKey,
            List<String> actions
    ) {
        List<String> defaults = defaultPermissions(moduleKey);
        Object permissions = metadata.get("permissions");
        if (!(permissions instanceof List<?> list) || !list.contains(moduleKey + ":view")) {
            metadata.put("permissions", defaults);
            actions.add("metadata.permissions.defaulted");
        }

        Object moduleObject = schema.get("module");
        Map<String, Object> module = moduleObject instanceof Map<?, ?> map
                ? copyMap((Map<String, Object>) map)
                : new LinkedHashMap<>();
        Object schemaPermissions = module.get("permissions");
        if (!(schemaPermissions instanceof List<?> list) || !list.contains(moduleKey + ":view")) {
            module.put("permissions", defaults);
            schema.put("module", module);
            actions.add("schema.module.permissions.defaulted");
        }
    }

    private void ensureMenuMetadata(
            Map<String, Object> metadata,
            AiGenerationTaskDO entity,
            List<String> actions
    ) {
        putIfBlank(metadata, "id", entity.getModuleKey(), "metadata.id.defaulted", actions);
        putIfBlank(metadata, "label", entity.getModuleName(), "metadata.label.defaulted", actions);
        putIfBlank(metadata, "icon", "sparkles", "metadata.icon.defaulted", actions);
        putIfBlank(metadata, "path", "/ai/" + entity.getModuleKey(), "metadata.path.defaulted", actions);
    }

    @SuppressWarnings("unchecked")
    private void ensureCrudDefaults(
            Map<String, Object> metadata,
            String moduleKey,
            String moduleName,
            Map<String, Object> schema,
            List<String> actions
    ) {
        Object crudObject = metadata.get("crud");
        if (!(crudObject instanceof Map<?, ?> crudMap)) {
            return;
        }
        Map<String, Object> crud = copyMap((Map<String, Object>) crudMap);
        putIfBlank(crud, "title", moduleName, "crud.title.defaulted", actions);
        putIfBlank(crud, "apiBase", "/" + moduleKey, "crud.apiBase.defaulted", actions);
        putIfBlank(crud, "restBase", "/rest/" + moduleKey, "crud.restBase.defaulted", actions);
        if (!crud.containsKey("workflow")) {
            Map<String, Object> rebuilt = metadataAssembler.toFrontendModuleMetadata(fakeTask(moduleKey, moduleName), schema);
            Object rebuiltCrud = rebuilt.get("crud");
            if (rebuiltCrud instanceof Map<?, ?> map) {
                crud.put("workflow", map.containsKey("workflow") ? map.get("workflow") : List.of());
            }
            actions.add("crud.workflow.rehydrated");
        }
        metadata.put("crud", crud);
    }

    private AiGenerationTaskDO fakeTask(String moduleKey, String moduleName) {
        AiGenerationTaskDO entity = new AiGenerationTaskDO();
        entity.setModuleKey(moduleKey);
        entity.setModuleName(moduleName);
        entity.setTaskNo("AUTO-REPAIR");
        return entity;
    }

    private void putIfBlank(
            Map<String, Object> map,
            String key,
            String value,
            String action,
            List<String> actions
    ) {
        Object current = map.get(key);
        if (current == null || String.valueOf(current).isBlank()) {
            map.put(key, value);
            actions.add(action);
        }
    }

    private List<String> defaultPermissions(String moduleKey) {
        return List.of(
                moduleKey + ":view",
                moduleKey + ":add",
                moduleKey + ":edit",
                moduleKey + ":delete",
                moduleKey + ":export"
        );
    }

    private Map<String, Object> copyMap(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    public record RepairAttempt(
            boolean changed,
            Map<String, Object> schema,
            Map<String, Object> metadata,
            List<String> actions
    ) {
    }
}
