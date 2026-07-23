package com.hrpm.service;


import com.hrpm.common.exception.WorkflowTemplateMissingException;
import com.hrpm.common.IdGenerator;
import com.hrpm.entity.LeaveRequestSubmission;
import com.hrpm.entity.WorkflowTemplate;
import com.hrpm.entity.WorkflowTemplateNode;
import com.hrpm.entity.WorkflowInstanceSnapshot;
import com.hrpm.entity.WorkflowInstance;
import com.hrpm.entity.WorkflowNodeSnapshot;
import com.hrpm.mapper.LeaveRequestMapper;
import com.hrpm.mapper.WorkflowMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveWorkflowService {
    private final WorkflowMapper workflowMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;
    private final WorkflowApproverResolver workflowApproverResolver;

    public LeaveWorkflowService(WorkflowMapper workflowMapper, LeaveRequestMapper leaveRequestMapper,
            IdGenerator idGenerator, ObjectMapper objectMapper, WorkflowApproverResolver workflowApproverResolver) {
        this.workflowMapper = workflowMapper;
        this.leaveRequestMapper = leaveRequestMapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
        this.workflowApproverResolver = workflowApproverResolver;
    }

    @Transactional
    public long submit(long userId, LeaveRequestSubmission request) {
        validate(request);
        if (request.workflowInstanceId() != null) {
            WorkflowInstance instance = workflowMapper.findInstance(request.workflowInstanceId());
            if (instance != null && "RETURNED".equals(instance.status()) && "LEAVE".equals(instance.businessType())
                    && instance.businessId() == request.id() && instance.initiatorUserId() == userId) {
                return resumeReturnedInstance(userId, request, instance);
            }
        }
        WorkflowTemplate template = workflowMapper.findLeaveTemplate(request.departmentId());
        if (template == null) {
            throw new WorkflowTemplateMissingException();
        }
        List<WorkflowNodeSnapshot> nodeSnapshots = workflowMapper.findNodes(template.id()).stream()
                .map(node -> {
                    JsonNode rule = parseRule(node);
                    return new WorkflowNodeSnapshot(node.nodeNo(), node.nodeType(), rule,
                            resolveAssignee(request, node, rule));
                })
                .toList();
        if (nodeSnapshots.isEmpty()) {
            throw new WorkflowTemplateMissingException();
        }
        WorkflowNodeSnapshot firstNode = nodeSnapshots.get(0);
        String templateSnapshot = serialize(new WorkflowInstanceSnapshot(
                template.id(), template.templateVersion(), request.employeeId(), request.departmentId(), nodeSnapshots));
        long instanceId = idGenerator.nextId();
        workflowMapper.insertInstance(instanceId, request.id(), userId,
                templateSnapshot, firstNode.nodeNo());
        workflowMapper.insertTask(idGenerator.nextId(), instanceId, firstNode.nodeNo(), serialize(firstNode), firstNode.assigneeUserId());
        if (leaveRequestMapper.markSubmitted(request.id(), request.employeeId(), request.version(), instanceId) != 1) {
            throw new IllegalStateException("Leave request changed before submission");
        }
        workflowMapper.insertActionLog(idGenerator.nextId(), instanceId, null, userId, "SUBMIT", "Leave request submitted");
        return instanceId;
    }

    private long resumeReturnedInstance(long userId, LeaveRequestSubmission request, WorkflowInstance instance) {
        WorkflowInstanceSnapshot snapshot = parseSnapshot(workflowMapper.findInstanceSnapshot(instance.id()));
        if (snapshot.nodes().isEmpty()) {
            throw new WorkflowTemplateMissingException();
        }
        WorkflowNodeSnapshot firstNode = snapshot.nodes().get(0);
        if (workflowMapper.resumeReturnedInstance(instance.id(), firstNode.nodeNo()) != 1) {
            throw new IllegalStateException("Workflow instance changed before resubmission");
        }
        workflowMapper.insertTask(idGenerator.nextId(), instance.id(), firstNode.nodeNo(), serialize(firstNode), firstNode.assigneeUserId());
        if (leaveRequestMapper.markSubmitted(request.id(), request.employeeId(), request.version(), instance.id()) != 1) {
            throw new IllegalStateException("Leave request changed before resubmission");
        }
        workflowMapper.insertActionLog(idGenerator.nextId(), instance.id(), null, userId, "RESUBMIT", "Leave request resubmitted");
        return instance.id();
    }

    private void validate(LeaveRequestSubmission request) {
        if (!"DRAFT".equals(request.status()) || !request.endTime().isAfter(request.startTime())) {
            throw new IllegalStateException("Leave request is not submittable");
        }
        if (!"FORMAL".equals(request.employmentStatus()) && !"PROBATION".equals(request.employmentStatus())) {
            throw new IllegalArgumentException("Employee cannot submit leave requests");
        }
        if (!"ACTIVE".equals(request.leaveTypeStatus())) {
            throw new IllegalArgumentException("Leave type is unavailable");
        }
        if (leaveRequestMapper.countOverlaps(request.employeeId(), request.id(), request.startTime(), request.endTime()) > 0) {
            throw new IllegalArgumentException("Leave request overlaps an existing request");
        }
        if (request.deductBalance()) {
            BigDecimal available = leaveRequestMapper.findAvailableBalance(request.employeeId(), request.balanceType(),
                    request.startTime().atZone(ZoneOffset.UTC).getYear());
            if (available == null || available.compareTo(request.durationHours()) < 0) {
                throw new IllegalArgumentException("Leave balance is insufficient");
            }
        }
    }

    private JsonNode parseRule(WorkflowTemplateNode node) {
        try {
            return objectMapper.readTree(node.approverRule());
        } catch (Exception exception) {
            throw new WorkflowTemplateMissingException();
        }
    }

    private long resolveAssignee(LeaveRequestSubmission request, WorkflowTemplateNode node, JsonNode rule) {
        String ruleType = rule.path("type").asText(node.nodeType());
        if ("DIRECT_MANAGER".equals(ruleType)) {
            Long managerUserId = workflowApproverResolver.resolveDirectManagerUserId(request.employeeId(), null);
            if (managerUserId == null) {
                throw new IllegalArgumentException("Leave request requires a direct manager approver");
            }
            return managerUserId;
        }
        return workflowApproverResolver.resolve(node, request.employeeId(), request.departmentId());
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create workflow snapshot", exception);
        }
    }

    private WorkflowInstanceSnapshot parseSnapshot(String value) {
        try {
            return objectMapper.readValue(value, WorkflowInstanceSnapshot.class);
        } catch (Exception exception) {
            throw new WorkflowTemplateMissingException();
        }
    }
}