package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.AiClarificationResponse;
import com.zhoolg.manage.entity.dto.AiGenerateRequest;
import com.zhoolg.manage.exception.ApiException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiRequirementService {
    private static final int MAX_DESCRIPTION_LENGTH = 4000;
    private static final int MAX_MODULE_KEY_LENGTH = 64;
    private static final int MAX_MODULE_NAME_LENGTH = 64;

    public Requirement normalize(AiGenerateRequest request) {
        String description = requireDescription(request.description());
        String moduleKey = normalizeModuleKey(request.moduleKey(), description);
        String moduleName = normalizeModuleName(request.moduleName(), description);
        return new Requirement(description, moduleKey, moduleName);
    }

    public AiClarificationResponse clarify(AiGenerateRequest request) {
        Requirement requirement = normalize(request);
        String description = requirement.description();
        String normalized = description.toLowerCase(Locale.ROOT);
        List<AiClarificationResponse.Question> questions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (!containsAny(normalized, "字段", "信息", "记录", "姓名", "电话", "金额", "时间", "状态", "类型")) {
            questions.add(question(
                    "core_fields",
                    "这个模块至少需要哪些业务字段？",
                    "字段不清晰会导致 AI 只能生成通用表单，后续返工成本高。",
                    List.of("客户姓名、联系电话、跟进状态、下次回访时间", "报修人、房源、故障类型、维修人员、完成时间")
            ));
        }
        if (!containsAny(normalized, "状态", "流程", "审批", "派单", "完成", "关闭", "启用", "停用")) {
            questions.add(question(
                    "workflow",
                    "这个模块有没有状态流转或审批流程？",
                    "工作流会影响列表按钮、权限、审计日志和状态校验。",
                    List.of("待处理 -> 已派单 -> 处理中 -> 已完成 -> 已关闭", "草稿 -> 待审核 -> 已通过/已驳回")
            ));
        }
        if (!containsAny(normalized, "角色", "权限", "管理员", "运营", "客服", "查看", "编辑", "删除")) {
            questions.add(question(
                    "roles_permissions",
                    "哪些角色可以查看、创建、编辑或删除数据？",
                    "权限边界不明确时，只能采用保守默认授权策略。",
                    List.of("管理员全部权限，运营可新增编辑，查看者只读", "客服可跟进，主管可审核")
            ));
        }
        if (!containsAny(normalized, "搜索", "筛选", "导出", "列表", "排序", "统计")) {
            questions.add(question(
                    "page_behavior",
                    "列表页需要哪些搜索、筛选、导出或统计能力？",
                    "页面行为会影响索引建议、查询字段和前端交互设计。",
                    List.of("按状态、负责人、创建时间筛选，支持导出", "按客户姓名搜索，按下次跟进时间排序")
            ));
        }
        if (!containsAny(normalized, "必填", "唯一", "校验", "手机号", "身份证", "金额", "范围")) {
            questions.add(question(
                    "validation_rules",
                    "有哪些必填、唯一性或格式校验规则？",
                    "缺少校验规则会降低生成页面的数据质量。",
                    List.of("手机号必填且格式校验", "合同编号唯一，金额必须大于 0")
            ));
        }

        if (description.length() < 40) {
            warnings.add("需求描述偏短，建议补充字段、流程、角色和页面行为。");
        }
        if (request.provider() != null && !request.provider().isBlank()) {
            warnings.add("当前请求声明模型提供商：" + request.provider() + "，生成接口会继续兼容该格式。");
        }

        int qualityScore = Math.max(0, 100 - questions.size() * 15 - warnings.size() * 5);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("moduleKey", requirement.moduleKey());
        snapshot.put("moduleName", requirement.moduleName());
        snapshot.put("businessType", firstText(request.businessType(), "crud-workflow"));
        snapshot.put("descriptionLength", description.length());
        snapshot.put("detectedWorkflow", containsAny(normalized, "状态", "流程", "审批", "派单", "完成", "关闭"));
        snapshot.put("detectedPermission", containsAny(normalized, "角色", "权限", "管理员", "运营", "客服"));
        snapshot.put("detectedPageBehavior", containsAny(normalized, "搜索", "筛选", "导出", "列表", "排序", "统计"));

        return new AiClarificationResponse(!questions.isEmpty(), qualityScore, snapshot, questions, warnings);
    }

    private String requireDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new ApiException(400, "AI 生成描述不能为空");
        }
        String trimmed = description.trim();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ApiException(400, "AI 生成描述最长 " + MAX_DESCRIPTION_LENGTH + " 字符");
        }
        return trimmed;
    }

    private String normalizeModuleKey(String moduleKey, String description) {
        if (moduleKey != null && !moduleKey.isBlank()) {
            String normalized = moduleKey.trim()
                    .replaceAll("[^a-zA-Z0-9_]", "_")
                    .replaceAll("_+", "_")
                    .replaceAll("^_|_$", "")
                    .toLowerCase(Locale.ROOT);
            if (normalized.isBlank()) {
                throw new ApiException(400, "模块标识不能为空");
            }
            if (normalized.length() > MAX_MODULE_KEY_LENGTH) {
                throw new ApiException(400, "模块标识最长 " + MAX_MODULE_KEY_LENGTH + " 字符");
            }
            return normalized;
        }
        if (description.contains("报修")) {
            return "repair_order";
        }
        if (description.contains("合同")) {
            return "contract";
        }
        return "generated_module";
    }

    private String normalizeModuleName(String moduleName, String description) {
        if (moduleName != null && !moduleName.isBlank()) {
            String trimmed = moduleName.trim();
            if (trimmed.length() > MAX_MODULE_NAME_LENGTH) {
                throw new ApiException(400, "模块名称最长 " + MAX_MODULE_NAME_LENGTH + " 字符");
            }
            return trimmed;
        }
        if (description.contains("报修")) {
            return "报修管理";
        }
        if (description.contains("合同")) {
            return "合同管理";
        }
        return "AI 生成模块";
    }

    private AiClarificationResponse.Question question(
            String id,
            String question,
            String reason,
            List<String> examples
    ) {
        return new AiClarificationResponse.Question(id, question, reason, examples);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public record Requirement(
            String description,
            String moduleKey,
            String moduleName
    ) {
    }
}
