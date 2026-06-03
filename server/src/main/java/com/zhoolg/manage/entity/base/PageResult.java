package com.zhoolg.manage.entity.base;

import java.util.List;
import java.util.Map;

public record PageResult(List<Map<String, Object>> list, long total) {
}
