package com.zhoolg.manage.service;

import com.zhoolg.manage.infrastructure.crud.ResourceDefinition;
import com.zhoolg.manage.entity.base.PageResult;

import java.util.List;
import java.util.Map;

/**
 * CRUD 服务接口
 */
public interface ICrudService {
    PageResult page(ResourceDefinition resource, Map<String, String> params);
    Map<String, Object> create(ResourceDefinition resource, Map<String, Object> payload);
    Map<String, Object> update(ResourceDefinition resource, Object id, Map<String, Object> payload);
    void delete(ResourceDefinition resource, Object id);
    void batchDelete(ResourceDefinition resource, List<?> ids);
    Map<String, Object> updateStatus(ResourceDefinition resource, Object id, Map<String, Object> payload);
    Map<String, Object> transitionWorkflow(ResourceDefinition resource, Object id, String action);
}
