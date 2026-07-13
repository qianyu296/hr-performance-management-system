package com.hrpm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpm.common.exception.WorkflowTaskInvalidException;
import com.hrpm.entity.WorkflowTemplateNode;
import com.hrpm.mapper.WorkflowMapper;
import org.springframework.stereotype.Service;

@Service
public class WorkflowApproverResolver {
    private final WorkflowMapper workflowMapper;
    private final ObjectMapper objectMapper;

    public WorkflowApproverResolver(WorkflowMapper workflowMapper, ObjectMapper objectMapper) {
        this.workflowMapper = workflowMapper;
        this.objectMapper = objectMapper;
    }

    public long resolve(WorkflowTemplateNode node) {
        try {
            JsonNode rule = objectMapper.readTree(node.approverRule());
            String ruleType = rule.path("type").asText("SPECIFIC_USER");
            if (!"SPECIFIC_USER".equals(ruleType)) {
                throw new WorkflowTaskInvalidException();
            }
            JsonNode userId = rule.get("userId");
            if (userId == null || !userId.canConvertToLong()) {
                throw new WorkflowTaskInvalidException();
            }
            Long activeUserId = workflowMapper.findActiveUserId(userId.longValue());
            if (activeUserId == null) {
                throw new WorkflowTaskInvalidException();
            }
            return activeUserId;
        } catch (WorkflowTaskInvalidException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new WorkflowTaskInvalidException();
        }
    }
}
