package com.zhoolg.manage.infrastructure.crud;

import com.zhoolg.manage.entity.base.PageResult;

import java.util.List;
import java.util.Map;

public interface ResourceRepository {
    List<Map<String, Object>> findAll(String key);

    PageResult page(ResourceDefinition resource, Map<String, String> params);

    Map<String, Object> create(ResourceDefinition resource, Map<String, Object> payload);

    Map<String, Object> update(ResourceDefinition resource, Object id, Map<String, Object> payload);

    void delete(ResourceDefinition resource, Object id);

    Map<String, Object> transitionWorkflow(ResourceDefinition resource, Object id, Map<String, Object> transition);
}
