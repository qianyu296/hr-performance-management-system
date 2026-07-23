package com.hrpm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpm.common.exception.WorkflowTaskInvalidException;
import com.hrpm.entity.WorkflowTemplateNode;
import com.hrpm.mapper.WorkflowMapper;
import org.springframework.stereotype.Service;

@Service
public class WorkflowApproverResolver {
    private static final String HR_ROLE_CODE = "HR_SPECIALIST";

    private final WorkflowMapper workflowMapper;
    private final ObjectMapper objectMapper;

    public WorkflowApproverResolver(WorkflowMapper workflowMapper, ObjectMapper objectMapper) {
        this.workflowMapper = workflowMapper;
        this.objectMapper = objectMapper;
    }

    public long resolve(WorkflowTemplateNode node, long initiatorEmployeeId, long departmentId) {
        return resolve(node, initiatorEmployeeId, null, departmentId);
    }

    public long resolve(WorkflowTemplateNode node, Long initiatorEmployeeId, Long onboardingManagerEmployeeId, long departmentId) {
        try {
            JsonNode rule = objectMapper.readTree(node.approverRule());
            String ruleType = rule.path("type").asText(node.nodeType());
            if (!ruleType.equals(node.nodeType())) {
                throw new WorkflowTaskInvalidException();
            }
            return switch (ruleType) {
                case "SPECIFIC_USER" -> resolveSpecificUser(rule);
                case "DIRECT_MANAGER" -> requireUser(resolveDirectManagerUserId(initiatorEmployeeId, onboardingManagerEmployeeId));
                case "DEPARTMENT_LEADER" -> requireUser(resolveDepartmentLeaderUserId(departmentId));
                case "HR" -> resolveHr(rule);
                default -> throw new WorkflowTaskInvalidException();
            };
        } catch (WorkflowTaskInvalidException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new WorkflowTaskInvalidException();
        }
    }

    public Long resolveDirectManagerUserId(Long initiatorEmployeeId, Long onboardingManagerEmployeeId) {
        if (onboardingManagerEmployeeId != null) {
            Long managerUserId = workflowMapper.findActiveUserIdByEmployeeId(onboardingManagerEmployeeId);
            if (managerUserId != null) {
                return managerUserId;
            }
        }
        if (initiatorEmployeeId != null) {
            return workflowMapper.findDirectManagerUserId(initiatorEmployeeId);
        }
        return null;
    }

    public Long resolveDepartmentLeaderUserId(long departmentId) {
        return workflowMapper.findDepartmentLeaderUserId(departmentId);
    }

    private long resolveSpecificUser(JsonNode rule) {
        JsonNode userId = rule.get("userId");
        if (userId == null || !userId.canConvertToLong()) {
            throw new WorkflowTaskInvalidException();
        }
        return requireUser(workflowMapper.findActiveUserId(userId.longValue()));
    }

    private long resolveHr(JsonNode rule) {
        String configuredRoleCode = rule.path("roleCode").asText(HR_ROLE_CODE);
        String roleCode = normalizeHrRoleCode(configuredRoleCode);
        var userIds = workflowMapper.findActiveUserIdsByRoleCode(roleCode);
        if (userIds.isEmpty()) {
            throw new WorkflowTaskInvalidException();
        }
        return userIds.get(0);
    }

    private String normalizeHrRoleCode(String value) {
        if (value == null) {
            return HR_ROLE_CODE;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "HR".equals(trimmed)) {
            return HR_ROLE_CODE;
        }
        return trimmed;
    }

    private long requireUser(Long userId) {
        if (userId == null) {
            throw new WorkflowTaskInvalidException();
        }
        return userId;
    }
}