package com.hrpm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpm.common.IdGenerator;
import com.hrpm.common.exception.ResourceNotFoundException;
import com.hrpm.common.exception.WorkflowTaskInvalidException;
import com.hrpm.entity.WorkflowBusinessContext;
import com.hrpm.entity.WorkflowInstance;
import com.hrpm.entity.WorkflowInstanceSnapshot;
import com.hrpm.entity.WorkflowNodeSnapshot;
import com.hrpm.entity.WorkflowTask;
import com.hrpm.entity.WorkflowTaskListRow;
import com.hrpm.mapper.WorkflowMapper;
import com.hrpm.vo.WorkflowActionLogVO;
import com.hrpm.vo.WorkflowInstanceDetailVO;
import com.hrpm.vo.WorkflowTaskListVO;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowTaskService {
    private final WorkflowMapper workflowMapper;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;
    private final WorkflowBusinessHandlerRegistry handlerRegistry;

    @Autowired
    public WorkflowTaskService(WorkflowMapper workflowMapper, IdGenerator idGenerator, ObjectMapper objectMapper,
            WorkflowBusinessHandlerRegistry handlerRegistry) {
        this.workflowMapper = workflowMapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
        this.handlerRegistry = handlerRegistry;
    }

    @Transactional
    public String approve(long userId, long taskId, int version, String comment) {
        WorkflowTask task = requirePendingTask(userId, taskId);
        if (task.version() != version || workflowMapper.approveTask(taskId, task.assigneeUserId(), version) != 1) {
            throw new IllegalStateException("Workflow task changed before approval");
        }
        WorkflowNodeSnapshot nextNode = nextNode(task.instanceId(), task.nodeNo());
        if (nextNode != null) {
            if (workflowMapper.advanceInstance(task.instanceId(), nextNode.nodeNo()) != 1) {
                throw new IllegalStateException("Workflow instance changed before approval");
            }
            workflowMapper.insertTask(idGenerator.nextId(), task.instanceId(), nextNode.nodeNo(), serialize(nextNode), nextNode.assigneeUserId());
            workflowMapper.insertActionLog(idGenerator.nextId(), task.instanceId(), taskId, userId, "APPROVE", comment);
            return "IN_PROGRESS";
        }
        handlerRegistry.require(task.businessType()).approve(context(task, userId));
        if (workflowMapper.approveInstance(task.instanceId()) != 1) {
            throw new IllegalStateException("Workflow instance changed before approval");
        }
        workflowMapper.insertActionLog(idGenerator.nextId(), task.instanceId(), taskId, userId, "APPROVE", comment);
        return "APPROVED";
    }

    public List<WorkflowTaskListVO> listPending(long userId) {
        List<WorkflowTaskListRow> rows = workflowMapper.listPendingTasks(userId);
        return rows.stream().map(WorkflowTaskListRow::toItem).toList();
    }

    public WorkflowInstanceDetailVO detail(long userId, long id) {
        WorkflowInstance instance = resolveAccessibleInstance(userId, id);
        List<WorkflowActionLogVO> history = workflowMapper.listActionLogs(instance.id()).stream().map(WorkflowActionLogVO::from).toList();
        return WorkflowInstanceDetailVO.from(instance, history);
    }

    @Transactional
    public String reject(long userId, long taskId, int version, String comment) {
        WorkflowTask task = requirePendingTask(userId, taskId);
        if (task.version() != version || workflowMapper.rejectTask(taskId, task.assigneeUserId(), version) != 1) {
            throw new IllegalStateException("Workflow task changed before rejection");
        }
        handlerRegistry.require(task.businessType()).reject(context(task, userId));
        if (workflowMapper.rejectInstance(task.instanceId()) != 1) {
            throw new IllegalStateException("Workflow instance changed before rejection");
        }
        workflowMapper.insertActionLog(idGenerator.nextId(), task.instanceId(), taskId, userId, "REJECT", comment);
        return "REJECTED";
    }

    @Transactional
    public String returnToInitiator(long userId, long taskId, int version, String comment) {
        WorkflowTask task = requirePendingTask(userId, taskId);
        if (task.version() != version || workflowMapper.returnTask(taskId, task.assigneeUserId(), version) != 1) {
            throw new IllegalStateException("Workflow task changed before return");
        }
        handlerRegistry.require(task.businessType()).returnToDraft(context(task, userId));
        if (workflowMapper.returnInstance(task.instanceId()) != 1) {
            throw new IllegalStateException("Workflow instance changed before return");
        }
        workflowMapper.insertActionLog(idGenerator.nextId(), task.instanceId(), taskId, userId, "RETURN", comment);
        return "RETURNED";
    }

    @Transactional
    public String transfer(long userId, long taskId, int version, long transferToUserId, String comment) {
        WorkflowTask task = workflowMapper.findTask(taskId);
        if (task == null || !"PENDING".equals(task.status()) || task.assigneeUserId() == transferToUserId
                || workflowMapper.findActiveUserId(transferToUserId) == null) {
            throw new WorkflowTaskInvalidException();
        }
        if (task.version() != version || workflowMapper.transferTask(taskId, version) != 1) {
            throw new IllegalStateException("Workflow task changed before transfer");
        }
        WorkflowNodeSnapshot node = taskSnapshot(task.nodeSnapshot());
        WorkflowNodeSnapshot transferredNode = new WorkflowNodeSnapshot(node.nodeNo(), node.nodeType(), node.approverRule(), transferToUserId);
        workflowMapper.insertTask(idGenerator.nextId(), task.instanceId(), task.nodeNo(), serialize(transferredNode), transferToUserId);
        workflowMapper.insertActionLog(idGenerator.nextId(), task.instanceId(), taskId, userId, "TRANSFER", comment);
        return "IN_PROGRESS";
    }

    @Transactional
    public String withdraw(long userId, long instanceId, int version, String comment) {
        WorkflowInstance instance = workflowMapper.findInstance(instanceId);
        if (instance == null || instance.initiatorUserId() != userId || !"IN_PROGRESS".equals(instance.status()) || instance.version() != version) {
            throw new WorkflowTaskInvalidException();
        }
        Long taskId = workflowMapper.findPendingTaskId(instanceId);
        if (workflowMapper.withdrawPendingTasks(instanceId) != 1) {
            throw new IllegalStateException("Workflow task changed before withdrawal");
        }
        handlerRegistry.require(instance.businessType())
                .withdraw(new WorkflowBusinessContext(instance.id(), instance.businessType(), instance.businessId(), userId));
        if (workflowMapper.withdrawInstance(instanceId, userId, version) != 1) {
            throw new IllegalStateException("Workflow instance changed before withdrawal");
        }
        workflowMapper.insertActionLog(idGenerator.nextId(), instanceId, taskId, userId, "WITHDRAW", comment);
        return "WITHDRAWN";
    }

    private WorkflowNodeSnapshot nextNode(long instanceId, int currentNodeNo) {
        try {
            WorkflowInstanceSnapshot snapshot = objectMapper.readValue(
                    workflowMapper.findInstanceSnapshot(instanceId), WorkflowInstanceSnapshot.class);
            return snapshot.nodes().stream()
                    .filter(node -> node.nodeNo() > currentNodeNo)
                    .findFirst()
                    .orElse(null);
        } catch (Exception exception) {
            throw new WorkflowTaskInvalidException();
        }
    }

    private WorkflowTask requirePendingTask(long userId, long taskId) {
        WorkflowTask task = workflowMapper.findTask(taskId);
        if (task == null || !"PENDING".equals(task.status()) || !canOperateTask(userId, task)) {
            throw new WorkflowTaskInvalidException();
        }
        return task;
    }

    private boolean canOperateTask(long userId, WorkflowTask task) {
        return task.assigneeUserId() == userId;
    }

    private WorkflowInstance resolveAccessibleInstance(long userId, long id) {
        WorkflowInstance instance = workflowMapper.findInstance(id);
        if (instance == null) {
            WorkflowTask task = workflowMapper.findTask(id);
            if (task != null) {
                instance = workflowMapper.findInstance(task.instanceId());
            }
        }
        if (instance == null || workflowMapper.countInstanceAccess(instance.id(), userId) == 0) {
            throw new ResourceNotFoundException("Workflow instance not found");
        }
        return instance;
    }

    private WorkflowNodeSnapshot taskSnapshot(String value) {
        try {
            return objectMapper.readValue(value, WorkflowNodeSnapshot.class);
        } catch (Exception exception) {
            throw new WorkflowTaskInvalidException();
        }
    }

    private String serialize(WorkflowNodeSnapshot node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create workflow task snapshot", exception);
        }
    }

    private WorkflowBusinessContext context(WorkflowTask task, long actorUserId) {
        return new WorkflowBusinessContext(task.instanceId(), task.businessType(), task.businessId(), actorUserId);
    }
}