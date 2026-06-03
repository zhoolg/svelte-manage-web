package com.zhoolg.manage.infrastructure.crud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FieldMaps {
    private FieldMaps() {
    }

    public static Map<String, Object> column(String field, String label, Object width, String format) {
        Map<String, Object> map = base("field", field, "label", label);
        if (width != null) {
            map.put(width instanceof Number ? "width" : "minWidth", width);
        }
        if (format != null) {
            map.put("format", format);
        }
        return map;
    }

    public static Map<String, Object> statusColumn(String field, String label, Map<?, ?> statusMap) {
        Map<String, Object> map = column(field, label, 100, "status");
        map.put("statusMap", statusMap);
        return map;
    }

    public static Map<String, Object> search(String field, String label, String type) {
        return base("field", field, "label", label, "type", type, "placeholder", "请输入" + label);
    }

    public static Map<String, Object> selectSearch(String field, String label, List<Map<String, Object>> options) {
        Map<String, Object> map = base("field", field, "label", label, "type", "select");
        map.put("options", options);
        return map;
    }

    public static Map<String, Object> form(String field, String label, String type, boolean required) {
        Map<String, Object> map = base("field", field, "label", label, "type", type);
        if (required) {
            map.put("required", true);
        }
        map.put("placeholder", "请输入" + label);
        return map;
    }

    public static Map<String, Object> selectForm(String field, String label, boolean required, Object defaultValue, List<Map<String, Object>> options) {
        Map<String, Object> map = base("field", field, "label", label, "type", "select");
        if (required) {
            map.put("required", true);
        }
        if (defaultValue != null) {
            map.put("defaultValue", defaultValue);
        }
        map.put("options", options);
        return map;
    }

    public static Map<String, Object> option(String label, Object value) {
        return base("label", label, "value", value);
    }

    public static List<Map<String, Object>> options(Object... pairs) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.add(option(String.valueOf(pairs[i]), pairs[i + 1]));
        }
        return result;
    }

    public static Map<String, Object> base(Object... pairs) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
