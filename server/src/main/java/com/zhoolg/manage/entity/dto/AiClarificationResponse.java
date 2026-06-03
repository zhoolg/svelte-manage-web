package com.zhoolg.manage.entity.dto;

import java.util.List;
import java.util.Map;

public record AiClarificationResponse(
        boolean needsClarification,
        int qualityScore,
        Map<String, Object> requirementsSnapshot,
        List<Question> questions,
        List<String> warnings
) {
    public record Question(
            String id,
            String question,
            String reason,
            List<String> examples
    ) {
    }
}
