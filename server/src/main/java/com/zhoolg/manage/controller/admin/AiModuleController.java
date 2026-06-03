package com.zhoolg.manage.controller.admin;

import com.zhoolg.manage.entity.base.ApiResponse;
import com.zhoolg.manage.entity.dto.AiApplyPlan;
import com.zhoolg.manage.entity.dto.AiClarificationResponse;
import com.zhoolg.manage.entity.dto.AiGenerateRequest;
import com.zhoolg.manage.entity.dto.AiGenerateResponse;
import com.zhoolg.manage.entity.dto.AiGenerationTaskSummary;
import com.zhoolg.manage.entity.dto.AiModuleVersionSummary;
import com.zhoolg.manage.entity.dto.AiProviderConfigRequest;
import com.zhoolg.manage.entity.dto.AiProviderConfigResponse;
import com.zhoolg.manage.entity.dto.AiPreviewResponse;
import com.zhoolg.manage.entity.base.CurrentUser;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.service.AiGenerationService;
import com.zhoolg.manage.service.AiUserProviderConfigService;
import com.zhoolg.manage.service.AuditLogService;
import com.zhoolg.manage.service.DynamicModuleService;
import com.zhoolg.manage.service.IAuthService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 模块生成控制器。
 * 请求体兼容本地格式、OpenAI Chat/Responses 格式和 Claude Messages 格式。
 */
@RestController
@RequestMapping("/admin/ai/modules")
public class AiModuleController {
    private static final long GENERATE_STREAM_TIMEOUT_MS = 10 * 60 * 1000L;

    private final AiGenerationService aiGenerationService;
    private final AiUserProviderConfigService providerConfigService;
    private final DynamicModuleService dynamicModuleService;
    private final IAuthService authService;
    private final AuditLogService auditLogService;

    public AiModuleController(
            AiGenerationService aiGenerationService,
            AiUserProviderConfigService providerConfigService,
            DynamicModuleService dynamicModuleService,
            IAuthService authService,
            AuditLogService auditLogService
    ) {
        this.aiGenerationService = aiGenerationService;
        this.providerConfigService = providerConfigService;
        this.dynamicModuleService = dynamicModuleService;
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/generate")
    public ApiResponse<AiGenerateResponse> generate(@RequestBody AiGenerateRequest request) {
        CurrentUser user = requireAiModulePermission();
        AiGenerateResponse response = aiGenerationService.generate(providerConfigService.applySavedConfig(user.id(), request));
        auditLogService.success(user, "ai", "generate", "ai_task", response.taskNo(), "生成 AI 模块草稿 " + response.taskNo());
        return ApiResponse.ok(response);
    }

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateStream(@RequestBody AiGenerateRequest request) {
        CurrentUser user = requireAiModulePermission();
        AiGenerateRequest effectiveRequest = providerConfigService.applySavedConfig(user.id(), request);
        SseEmitter emitter = new SseEmitter(GENERATE_STREAM_TIMEOUT_MS);
        AtomicBoolean running = new AtomicBoolean(true);

        CompletableFuture.runAsync(() -> {
            while (running.get()) {
                try {
                    Thread.sleep(15000);
                    if (running.get()) {
                        sendProgress(emitter, "schema", "AI 仍在生成模块草稿，请稍候", 45, "running");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (IOException ignored) {
                    running.set(false);
                    return;
                }
            }
        });

        CompletableFuture.runAsync(() -> {
            try {
                AiGenerateResponse response = aiGenerationService.generate(effectiveRequest, progress -> {
                    try {
                        sendProgress(emitter, progress);
                    } catch (IOException ex) {
                        throw new ApiException(499, "客户端已断开生成进度连接");
                    }
                });
                auditLogService.success(user, "ai", "generate", "ai_task", response.taskNo(), "生成 AI 模块草稿 " + response.taskNo());
                sendEvent(emitter, "done", ApiResponse.ok(response));
                running.set(false);
                emitter.complete();
            } catch (Exception ex) {
                running.set(false);
                sendGenerateError(emitter, ex);
            }
        });

        return emitter;
    }

    @PostMapping("/clarify")
    public ApiResponse<AiClarificationResponse> clarify(@RequestBody AiGenerateRequest request) {
        CurrentUser user = requireAiModulePermission();
        return ApiResponse.ok(aiGenerationService.clarify(providerConfigService.applySavedConfig(user.id(), request)));
    }

    @GetMapping("/provider-config")
    public ApiResponse<AiProviderConfigResponse> providerConfig() {
        CurrentUser user = requireAiModulePermission();
        return ApiResponse.ok(providerConfigService.get(user.id()));
    }

    @PutMapping("/provider-config")
    public ApiResponse<AiProviderConfigResponse> saveProviderConfig(@RequestBody AiProviderConfigRequest request) {
        CurrentUser user = requireAiModulePermission();
        AiProviderConfigResponse response = providerConfigService.save(user.id(), request);
        auditLogService.success(user, "ai", "save_provider_config", "admin_user", user.id(), "保存 AI 模型配置");
        return ApiResponse.ok(response, "AI 模型配置已保存");
    }

    @GetMapping("/tasks")
    public ApiResponse<List<AiGenerationTaskSummary>> tasks(@RequestParam(defaultValue = "20") int limit) {
        requireAiModulePermission();
        return ApiResponse.ok(aiGenerationService.recentTasks(limit));
    }

    @GetMapping("/{taskNo}/preview")
    public ApiResponse<AiPreviewResponse> preview(@PathVariable String taskNo) {
        requireAiModulePermission();
        return ApiResponse.ok(aiGenerationService.preview(taskNo));
    }

    @GetMapping("/{taskNo}/plan")
    public ApiResponse<AiApplyPlan> plan(@PathVariable String taskNo) {
        requireAiModulePermission();
        return ApiResponse.ok(aiGenerationService.applyPlan(taskNo));
    }

    @PostMapping("/{taskNo}/apply")
    public ApiResponse<Map<String, Object>> apply(@PathVariable String taskNo) {
        CurrentUser user = requireAiModulePermission();
        Map<String, Object> metadata = aiGenerationService.apply(taskNo);
        auditLogService.success(user, "ai", "apply", "ai_task", taskNo, "应用 AI 模块 " + metadata.get("id"));
        return ApiResponse.ok(metadata, "应用成功");
    }

    @PostMapping("/{taskNo}/rollback")
    public ApiResponse<Map<String, Object>> rollback(@PathVariable String taskNo) {
        CurrentUser user = requireAiModulePermission();
        Map<String, Object> result = aiGenerationService.rollback(taskNo);
        auditLogService.success(user, "ai", "rollback", "ai_task", taskNo, "回滚 AI 模块 " + result.get("moduleKey"));
        return ApiResponse.ok(result, "回滚成功");
    }

    @GetMapping("/{moduleKey}/versions")
    public ApiResponse<List<AiModuleVersionSummary>> versions(@PathVariable String moduleKey) {
        requireAiModulePermission();
        return ApiResponse.ok(dynamicModuleService.versions(moduleKey));
    }

    @GetMapping("/{moduleKey}/designer")
    public ApiResponse<Map<String, Object>> designerMetadata(@PathVariable String moduleKey) {
        requireAiModulePermission();
        return ApiResponse.ok(dynamicModuleService.designerMetadata(moduleKey));
    }

    @PutMapping("/{moduleKey}/designer")
    public ApiResponse<Map<String, Object>> saveDesignerMetadata(
            @PathVariable String moduleKey,
            @RequestBody Map<String, Object> metadata
    ) {
        CurrentUser user = requireAiModulePermission();
        Map<String, Object> result = dynamicModuleService.saveDesignerMetadata(moduleKey, metadata);
        auditLogService.success(user, "ai", "designer_save", "dynamic_module", moduleKey, "保存模块设计器配置 " + moduleKey);
        return ApiResponse.ok(result, "设计器配置已保存");
    }

    @PostMapping("/{moduleKey}/versions/{versionNo}/restore")
    public ApiResponse<Map<String, Object>> restoreVersion(
            @PathVariable String moduleKey,
            @PathVariable int versionNo
    ) {
        CurrentUser user = requireAiModulePermission();
        Map<String, Object> result = dynamicModuleService.restoreVersion(moduleKey, versionNo);
        auditLogService.success(user, "ai", "restore", "dynamic_module", moduleKey, "恢复 AI 模块版本 v" + versionNo);
        return ApiResponse.ok(result, "版本已恢复");
    }

    private CurrentUser requireAiModulePermission() {
        CurrentUser user = authService.requireUser();
        authService.requirePermission(user, "settings:edit");
        return user;
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) throws IOException {
        synchronized (emitter) {
            emitter.send(SseEmitter.event().name(name).data(data));
        }
    }

    private void sendProgress(
            SseEmitter emitter,
            String phase,
            String message,
            int progress,
            String status
    ) throws IOException {
        sendEvent(emitter, "progress", Map.of(
                "phase", phase,
                "message", message,
                "progress", progress,
                "status", status
        ));
    }

    private void sendProgress(SseEmitter emitter, AiGenerationService.GenerationProgress progress) throws IOException {
        sendProgress(emitter, progress.phase(), progress.message(), progress.progress(), progress.status());
    }

    private void sendGenerateError(SseEmitter emitter, Exception ex) {
        try {
            int code = ex instanceof ApiException apiException ? apiException.getCode() : 500;
            String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "生成失败" : ex.getMessage();
            sendEvent(emitter, "error", ApiResponse.fail(code, message));
            emitter.complete();
        } catch (IOException ioException) {
            emitter.completeWithError(ioException);
        }
    }
}
