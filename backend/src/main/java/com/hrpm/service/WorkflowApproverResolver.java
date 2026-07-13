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

    public long resolve(WorkflowTemplateNode node, long initiatorEmployeeId, long departmentId) {
        try {
            JsonNode rule = objectMapper.readTree(node.approverRule());
            String ruleType = rule.path("type").asText(node.nodeType());
            if (!ruleType.equals(node.nodeType())) {
                throw new WorkflowTaskInvalidException();
            }
            return switch (ruleType) {
                case "SPECIFIC_USER" -> resolveSpecificUser(rule);
                case "DIRECT_MANAGER" -> requireUser(workflowMapper.findDirectManagerUserId(initiatorEmployeeId));
                case "DEPARTMENT_LEADER" -> requireUser(workflowMapper.findDepartmentLeaderUserId(departmentId));
                case "HR" -> resolveHr(rule);
                default -> throw new WorkflowTaskInvalidException();
            };
        } catch (WorkflowTaskInvalidException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new WorkflowTaskInvalidException();
        }
    }

    private long resolveSpecificUser(JsonNode rule) {
        JsonNode userId = rule.get("userId");
        if (userId == null || !userId.canConvertToLong()) {
            throw new WorkflowTaskInvalidException();
        }
        return requireUser(workflowMapper.findActiveUserId(userId.longValue()));
    }

    private long resolveHr(JsonNode rule) {
        String roleCode = rule.path("roleCode").asText("HR");
        var userIds = workflowMapper.findActiveUserIdsByRoleCode(roleCode);
        if (userIds.size() != 1) {
            throw new WorkflowTaskInvalidException();
        }
        return userIds.get(0);
    }

    private long requireUser(Long userId) {
        if (userId == null) {
            throw new WorkflowTaskInvalidException();
        }
        return userId;
    }
}
