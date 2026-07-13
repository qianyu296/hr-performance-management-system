package com.hrpm.service;


import com.hrpm.common.exception.WorkflowTemplateMissingException;
import com.hrpm.common.IdGenerator;
import com.hrpm.entity.LeaveRequestSubmission;
import com.hrpm.entity.WorkflowTemplate;
import com.hrpm.entity.WorkflowTemplateNode;
import com.hrpm.mapper.LeaveRequestMapper;
import com.hrpm.mapper.WorkflowMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveWorkflowService {
    private final WorkflowMapper workflowMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public LeaveWorkflowService(WorkflowMapper workflowMapper, LeaveRequestMapper leaveRequestMapper,
            IdGenerator idGenerator, ObjectMapper objectMapper) {
        this.workflowMapper = workflowMapper;
        this.leaveRequestMapper = leaveRequestMapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public long submit(long userId, LeaveRequestSubmission request) {
        validate(request);
        WorkflowTemplate template = workflowMapper.findLeaveTemplate(request.departmentId());
        if (template == null) {
            throw new WorkflowTemplateMissingException();
        }
        WorkflowTemplateNode node = workflowMapper.findFirstNode(template.id());
        long assigneeUserId = assigneeUserId(node);
        long instanceId = idGenerator.nextId();
        workflowMapper.insertInstance(instanceId, request.id(), userId,
                "{\"templateId\":" + template.id() + ",\"templateVersion\":" + template.templateVersion() + "}", node.nodeNo());
        workflowMapper.insertTask(idGenerator.nextId(), instanceId, node.nodeNo(), node.approverRule(), assigneeUserId);
        if (leaveRequestMapper.markSubmitted(request.id(), request.employeeId(), request.version(), instanceId) != 1) {
            throw new IllegalStateException("Leave request changed before submission");
        }
        return instanceId;
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

    private long assigneeUserId(WorkflowTemplateNode node) {
        if (node == null) {
            throw new WorkflowTemplateMissingException();
        }
        try {
            JsonNode userId = objectMapper.readTree(node.approverRule()).get("userId");
            if (userId == null || !userId.canConvertToLong()) {
                throw new WorkflowTemplateMissingException();
            }
            return userId.longValue();
        } catch (Exception exception) {
            if (exception instanceof WorkflowTemplateMissingException workflowException) {
                throw workflowException;
            }
            throw new WorkflowTemplateMissingException();
        }
    }
}
