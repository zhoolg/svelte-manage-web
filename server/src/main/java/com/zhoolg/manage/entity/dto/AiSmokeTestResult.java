package com.zhoolg.manage.entity.dto;

import java.util.List;

public record AiSmokeTestResult(
        boolean passed,
        int score,
        String executedAt,
        List<Check> checks,
        List<Diagnostic> diagnostics,
        List<String> repairSuggestions
) {
    public AiSmokeTestResult(boolean passed, int score, String executedAt, List<Check> checks) {
        this(passed, score, executedAt, checks, List.of(), List.of());
    }

    public record Check(
            String code,
            String target,
            String status,
            String message
    ) {
    }

    public record Diagnostic(
            String code,
            String target,
            String severity,
            String cause,
            String suggestion
    ) {
    }
}
