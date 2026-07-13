package com.hrpm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpm.common.IdGenerator;
import com.hrpm.common.exception.WorkflowTemplateMissingException;
import com.hrpm.entity.OvertimeRequestSubmission;
import com.hrpm.entity.WorkflowInstance;
import com.hrpm.entity.WorkflowInstanceSnapshot;
import com.hrpm.entity.WorkflowNodeSnapshot;
import com.hrpm.entity.WorkflowTemplate;
import com.hrpm.entity.WorkflowTemplateNode;
import com.hrpm.mapper.OvertimeRequestMapper;
import com.hrpm.mapper.WorkflowMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OvertimeWorkflowService {
    private static final String BUSINESS_TYPE = "OVERTIME";
    private final WorkflowMapper workflowMapper;
    private final OvertimeRequestMapper overtimeRequestMapper;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;
    private final WorkflowApproverResolver approverResolver;

    public OvertimeWorkflowService(WorkflowMapper workflowMapper, OvertimeRequestMapper overtimeRequestMapper,
                                   IdGenerator idGenerator, ObjectMapper objectMapper,
                                   WorkflowApproverResolver approverResolver) {
        this.workflowMapper = workflowMapper;
        this.overtimeRequestMapper = overtimeRequestMapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
        this.approverResolver = approverResolver;
    }

    @Transactional
    public long submit(long userId, OvertimeRequestSubmission request) {
        validate(request);
        if (request.workflowInstanceId() != null) {
            WorkflowInstance instance = workflowMapper.findInstance(request.workflowInstanceId());
            if (instance != null && "RETURNED".equals(instance.status()) && BUSINESS_TYPE.equals(instance.businessType())
                    && instance.businessId() == request.id() && instance.initiatorUserId() == userId) {
                return resumeReturned(userId, request, instance);
            }
        }
        WorkflowTemplate template = workflowMapper.findTemplateForBusiness(BUSINESS_TYPE, request.departmentId());
        if (template == null) throw new WorkflowTemplateMissingException();
        List<WorkflowNodeSnapshot> nodes = workflowMapper.findNodes(template.id()).stream()
                .map(node -> new WorkflowNodeSnapshot(node.nodeNo(), node.nodeType(), parseRule(node),
                        approverResolver.resolve(node, request.employeeId(), request.departmentId())))
                .toList();
        if (nodes.isEmpty()) throw new WorkflowTemplateMissingException();
        WorkflowNodeSnapshot first = nodes.get(0);
        long instanceId = idGenerator.nextId();
        workflowMapper.insertBusinessInstance(instanceId, BUSINESS_TYPE, request.id(), userId,
                serialize(new WorkflowInstanceSnapshot(template.id(), template.templateVersion(), request.employeeId(), request.departmentId(), nodes)),
                first.nodeNo());
        workflowMapper.insertTask(idGenerator.nextId(), instanceId, first.nodeNo(), serialize(first), first.assigneeUserId());
        if (overtimeRequestMapper.markSubmitted(request.id(), request.employeeId(), request.version(), instanceId) != 1) {
            throw new IllegalStateException("Overtime request changed before submission");
        }
        workflowMapper.insertActionLog(idGenerator.nextId(), instanceId, null, userId, "SUBMIT", "Overtime request submitted");
        return instanceId;
    }

    private long resumeReturned(long userId, OvertimeRequestSubmission request, WorkflowInstance instance) {
        WorkflowInstanceSnapshot snapshot = parseSnapshot(workflowMapper.findInstanceSnapshot(instance.id()));
        if (snapshot.nodes().isEmpty()) throw new WorkflowTemplateMissingException();
        WorkflowNodeSnapshot first = snapshot.nodes().get(0);
        if (workflowMapper.resumeReturnedInstance(instance.id(), first.nodeNo()) != 1
                || overtimeRequestMapper.markSubmitted(request.id(), request.employeeId(), request.version(), instance.id()) != 1) {
            throw new IllegalStateException("Overtime request changed before resubmission");
        }
        workflowMapper.insertTask(idGenerator.nextId(), instance.id(), first.nodeNo(), serialize(first), first.assigneeUserId());
        workflowMapper.insertActionLog(idGenerator.nextId(), instance.id(), null, userId, "RESUBMIT", "Overtime request resubmitted");
        return instance.id();
    }

    private void validate(OvertimeRequestSubmission request) {
        if (!"DRAFT".equals(request.status()) || !request.endTime().isAfter(request.startTime())) {
            throw new IllegalStateException("Overtime request is not submittable");
        }
        if (!"FORMAL".equals(request.employmentStatus()) && !"PROBATION".equals(request.employmentStatus())) {
            throw new IllegalArgumentException("Employee cannot submit overtime requests");
        }
    }

    private JsonNode parseRule(WorkflowTemplateNode node) {
        try { return objectMapper.readTree(node.approverRule()); } catch (Exception exception) { throw new WorkflowTemplateMissingException(); }
    }

    private String serialize(Object value) {
        try { return objectMapper.writeValueAsString(value); } catch (Exception exception) { throw new IllegalStateException("Unable to create workflow snapshot", exception); }
    }

    private WorkflowInstanceSnapshot parseSnapshot(String value) {
        try { return objectMapper.readValue(value, WorkflowInstanceSnapshot.class); } catch (Exception exception) { throw new WorkflowTemplateMissingException(); }
    }
}
