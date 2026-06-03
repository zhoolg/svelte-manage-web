package com.zhoolg.manage.service.impl;

import com.zhoolg.manage.entity.base.PageResult;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.service.ICrudService;
import com.zhoolg.manage.infrastructure.crud.ResourceDefinition;
import com.zhoolg.manage.infrastructure.crud.ResourceRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CrudServiceImpl implements ICrudService {
    private final ResourceRepository repository;

    public CrudServiceImpl(ResourceRepository repository) {
        this.repository = repository;
    }

    public PageResult page(ResourceDefinition resource, Map<String, String> params) {
        return repository.page(resource, params);
    }

    public Map<String, Object> create(ResourceDefinition resource, Map<String, Object> payload) {
        return repository.create(resource, pick(payload, resource.allowedCreateFields()));
    }

    public Map<String, Object> update(ResourceDefinition resource, Object id, Map<String, Object> payload) {
        Map<String, Object> data = pick(payload, resource.allowedUpdateFields());
        data.put("id", id);
        return repository.update(resource, id, data);
    }

    public void delete(ResourceDefinition resource, Object id) {
        repository.delete(resource, id);
    }

    public void batchDelete(ResourceDefinition resource, List<?> ids) {
        ids.forEach(id -> repository.delete(resource, id));
    }

    public Map<String, Object> updateStatus(ResourceDefinition resource, Object id, Map<String, Object> payload) {
        Object value = payload.containsKey("value") ? payload.get("value") : payload.getOrDefault("status", payload.get("enabled"));
        String field = String.valueOf(payload.getOrDefault("field", payload.containsKey("enabled") ? "enabled" : "status"));
        if (!resource.allowedUpdateFields().contains(field)) {
            throw new ApiException(400, "字段不允许修改");
        }
        return repository.update(resource, id, Map.of(field, value));
    }

    public Map<String, Object> transitionWorkflow(ResourceDefinition resource, Object id, String action) {
        Map<String, Object> transition = resource.workflow().stream()
                .filter(item -> action.equals(String.valueOf(item.get("action"))))
                .findFirst()
                .orElseThrow(() -> new ApiException(400, "工作流动作不存在"));
        String statusField = String.valueOf(transition.getOrDefault("statusField", "status"));
        if (!resource.allowedUpdateFields().contains(statusField)) {
            throw new ApiException(400, "工作流状态字段不允许修改");
        }
        return repository.transitionWorkflow(resource, id, transition);
    }

    private Map<String, Object> pick(Map<String, Object> payload, List<String> fields) {
        Map<String, Object> result = new LinkedHashMap<>();
        fields.forEach(field -> {
            if (payload.containsKey(field)) {
                result.put(field, payload.get(field));
            }
        });
        return result;
    }
}
