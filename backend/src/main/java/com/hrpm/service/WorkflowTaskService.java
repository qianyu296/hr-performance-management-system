package com.hrpm.service;


import com.hrpm.common.exception.WorkflowTaskInvalidException;
import com.hrpm.common.IdGenerator;
import com.hrpm.entity.LeaveBalanceRow;
import com.hrpm.entity.LeaveRequestSubmission;
import com.hrpm.entity.WorkflowTask;
import com.hrpm.entity.WorkflowTaskListRow;
import com.hrpm.entity.WorkflowTemplateNode;
import com.hrpm.mapper.LeaveRequestMapper;
import com.hrpm.mapper.WorkflowMapper;
import com.hrpm.vo.WorkflowTaskListVO;

import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class WorkflowTaskService {
    private final WorkflowMapper workflowMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public WorkflowTaskService(WorkflowMapper workflowMapper, LeaveRequestMapper leaveRequestMapper, IdGenerator idGenerator) {
        this(workflowMapper, leaveRequestMapper, idGenerator, new ObjectMapper());
    }

    @Autowired
    public WorkflowTaskService(WorkflowMapper workflowMapper, LeaveRequestMapper leaveRequestMapper, IdGenerator idGenerator, ObjectMapper objectMapper) {
        this.workflowMapper = workflowMapper;
        this.leaveRequestMapper = leaveRequestMapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public String approve(long userId, long taskId, int version, String comment) {
        WorkflowTask task = workflowMapper.findTask(taskId);
        if (task == null || task.assigneeUserId() != userId || !"PENDING".equals(task.status())) {
            throw new WorkflowTaskInvalidException();
        }
        if (task.version() != version || workflowMapper.approveTask(taskId, userId, version) != 1) {
            throw new IllegalStateException("Workflow task changed before approval");
        }
        WorkflowTemplateNode nextNode = workflowMapper.findNextNode(task.instanceId(), task.nodeNo());
        if (nextNode != null) {
            long assigneeUserId = assigneeUserId(nextNode);
            if (workflowMapper.advanceInstance(task.instanceId(), nextNode.nodeNo()) != 1) {
                throw new IllegalStateException("Workflow instance changed before approval");
            }
            workflowMapper.insertTask(idGenerator.nextId(), task.instanceId(), nextNode.nodeNo(), nextNode.approverRule(), assigneeUserId);
            workflowMapper.insertActionLog(idGenerator.nextId(), task.instanceId(), taskId, userId, "APPROVE", comment);
            return "IN_PROGRESS";
        }
        LeaveRequestSubmission request = leaveRequestMapper.findSubmission(task.businessId());
        if (request == null || !"IN_PROGRESS".equals(request.status())) {
            throw new IllegalStateException("Leave request is not in progress");
        }
        LeaveBalanceRow balance = leaveRequestMapper.findBalance(request.employeeId(), request.balanceType(), request.startTime().atZone(java.time.ZoneOffset.UTC).getYear());
        if (balance == null || balance.availableHours().compareTo(request.durationHours()) < 0) {
            throw new IllegalStateException("Leave balance is insufficient");
        }
        BigDecimal after = balance.availableHours().subtract(request.durationHours());
        if (leaveRequestMapper.approveRequest(request.id(), request.version()) != 1
                || leaveRequestMapper.updateBalance(balance.id(), balance.version(), after) != 1
                || workflowMapper.approveInstance(task.instanceId()) != 1) {
            throw new IllegalStateException("Leave request changed before approval");
        }
        leaveRequestMapper.insertBalanceChange(idGenerator.nextId(), balance.id(), request.employeeId(), request.balanceType(),
                request.durationHours().negate(), balance.availableHours(), after, request.id());
        workflowMapper.insertActionLog(idGenerator.nextId(), task.instanceId(), taskId, userId, "APPROVE", comment);
        return "APPROVED";
    }

    public List<WorkflowTaskListVO> listPending(long userId) {
        return workflowMapper.listPendingTasks(userId).stream().map(WorkflowTaskListRow::toItem).toList();
    }

    @Transactional
    public String reject(long userId, long taskId, int version, String comment) {
        WorkflowTask task = workflowMapper.findTask(taskId);
        if (task == null || task.assigneeUserId() != userId || !"PENDING".equals(task.status())) {
            throw new WorkflowTaskInvalidException();
        }
        if (task.version() != version || workflowMapper.rejectTask(taskId, userId, version) != 1) {
            throw new IllegalStateException("Workflow task changed before rejection");
        }
        LeaveRequestSubmission request = leaveRequestMapper.findSubmission(task.businessId());
        if (request == null || !"IN_PROGRESS".equals(request.status())) {
            throw new IllegalStateException("Leave request is not in progress");
        }
        if (leaveRequestMapper.rejectRequest(request.id(), request.version()) != 1
                || workflowMapper.rejectInstance(task.instanceId()) != 1) {
            throw new IllegalStateException("Leave request changed before rejection");
        }
        workflowMapper.insertActionLog(idGenerator.nextId(), task.instanceId(), taskId, userId, "REJECT", comment);
        return "REJECTED";
    }

    private long assigneeUserId(WorkflowTemplateNode node) {
        try {
            var userId = objectMapper.readTree(node.approverRule()).get("userId");
            if (userId == null || !userId.canConvertToLong()) throw new WorkflowTaskInvalidException();
            return userId.longValue();
        } catch (WorkflowTaskInvalidException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new WorkflowTaskInvalidException();
        }
    }
}
