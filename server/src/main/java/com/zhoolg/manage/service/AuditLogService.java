package com.zhoolg.manage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.entity.base.CurrentUser;
import com.zhoolg.manage.entity.pojo.AuditLogDO;
import com.zhoolg.manage.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class AuditLogService {
    private static final String REDACTED = "******";
    private static final List<String> SENSITIVE_KEYWORDS = List.of(
            "password", "passwd", "pwd", "token", "apikey", "api_key", "api-key",
            "secret", "authorization", "cookie", "credential", "privatekey", "private_key", "private-key"
    );
    private static final Pattern SENSITIVE_TEXT_PATTERN = Pattern.compile(
            "(?i)(password|passwd|pwd|access[_-]?token|refresh[_-]?token|token|api[_-]?key|secret|authorization|cookie|credential|private[_-]?key)\\s*[:=]\\s*([^,;\\s}\\]]+)"
    );

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    public void success(CurrentUser user, String module, String action, String targetType, Object targetId, String description) {
        record(user, module, action, targetType, targetId, "success", description, Map.of());
    }

    public void success(CurrentUser user, String module, String action, String targetType, Object targetId, String description, Map<String, Object> details) {
        record(user, module, action, targetType, targetId, "success", description, details);
    }

    public void failure(String username, String module, String action, String description) {
        record(null, username, module, action, null, null, "error", description, Map.of());
    }

    public void record(CurrentUser user, String module, String action, String targetType, Object targetId, String result, String description, Map<String, Object> details) {
        String username = user == null ? "anonymous" : user.username();
        Long userId = user == null ? null : user.id();
        record(userId, username, module, action, targetType, targetId, result, description, details);
    }

    private void record(Long userId, String username, String module, String action, String targetType, Object targetId, String result, String description, Map<String, Object> details) {
        HttpServletRequest request = currentRequest();
        AuditLogDO entity = new AuditLogDO();
        entity.setUserId(userId);
        entity.setUsername(limit(blankToDefault(username, "anonymous"), 64));
        entity.setModule(limit(blankToDefault(module, "system"), 64));
        entity.setAction(limit(blankToDefault(action, "unknown"), 64));
        entity.setTargetType(limit(targetType, 64));
        entity.setTargetId(targetId == null ? null : limit(String.valueOf(targetId), 128));
        entity.setResult("error".equals(result) ? "error" : "success");
        entity.setDescription(limit(redactText(blankToDefault(description, "操作记录")), 512));
        entity.setIp(limit(clientIp(request), 64));
        entity.setUserAgent(limit(request == null ? null : request.getHeader("User-Agent"), 512));
        entity.setDetailsJson(writeJson(sanitizeValue(details == null ? Map.of() : details)));

        auditLogMapper.insert(entity);
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private Object sanitizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                sanitized.put(key, isSensitiveKey(key) ? REDACTED : sanitizeValue(entry.getValue()));
            }
            return sanitized;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> sanitized = new ArrayList<>(collection.size());
            for (Object item : collection) {
                sanitized.add(sanitizeValue(item));
            }
            return sanitized;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> sanitized = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                sanitized.add(sanitizeValue(Array.get(value, i)));
            }
            return sanitized;
        }
        if (value instanceof CharSequence text) {
            return redactText(text.toString());
        }
        return value;
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key == null ? "" : key.toLowerCase().replaceAll("[\\s._-]", "");
        return SENSITIVE_KEYWORDS.stream()
                .map(keyword -> keyword.replaceAll("[\\s._-]", ""))
                .anyMatch(normalized::contains);
    }

    private String redactText(String value) {
        if (value == null) {
            return null;
        }
        return SENSITIVE_TEXT_PATTERN.matcher(value).replaceAll("$1=" + REDACTED);
    }
}
