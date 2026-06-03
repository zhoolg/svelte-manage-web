package com.zhoolg.manage.entity.dto;

import java.util.List;

public record AiApplyPlan(
        String taskNo,
        String moduleKey,
        String moduleName,
        boolean canApply,
        int riskScore,
        String riskLevel,
        List<Operation> operations,
        List<String> warnings
) {
    public record Operation(
            String category,
            String action,
            String target,
            String description,
            String riskLevel
    ) {
    }
}
