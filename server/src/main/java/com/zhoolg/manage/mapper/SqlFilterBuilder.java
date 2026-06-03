package com.zhoolg.manage.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class SqlFilterBuilder {
    private static final List<String> PAGE_PARAMS = List.of("pageNum", "pageSize", "page", "size");

    private SqlFilterBuilder() {
    }

    static Filter build(Map<String, String> params, Map<String, String> columns, List<String> searchableFields) {
        List<String> clauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String field = entry.getKey();
            String expected = entry.getValue();
            if (expected == null || expected.isBlank() || PAGE_PARAMS.contains(field)) {
                continue;
            }
            String column = columns.get(field);
            if (column == null) {
                clauses.add("1 = 0");
                continue;
            }
            if (searchableFields.contains(field)) {
                clauses.add(column + " LIKE ? ESCAPE '\\\\'");
                args.add("%" + escapeLike(expected.trim()) + "%");
            } else {
                clauses.add(column + " = ?");
                args.add(expected.trim());
            }
        }
        String whereClause = clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
        return new Filter(whereClause, args);
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    record Filter(String whereClause, List<Object> args) {
        Object[] withPage(int pageNum, int pageSize) {
            List<Object> pagedArgs = new ArrayList<>(args);
            pagedArgs.add(Math.max(pageSize, 1));
            pagedArgs.add(Math.max((pageNum - 1) * pageSize, 0));
            return pagedArgs.toArray();
        }

        Object[] argsArray() {
            return args.toArray();
        }
    }
}
