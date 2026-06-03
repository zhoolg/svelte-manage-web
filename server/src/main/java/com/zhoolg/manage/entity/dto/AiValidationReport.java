package com.zhoolg.manage.entity.dto;

import java.util.List;

public record AiValidationReport(
        boolean passed,
        int score,
        List<Issue> issues
) {
    public record Issue(
            String level,
            String code,
            String message
    ) {
    }
}
