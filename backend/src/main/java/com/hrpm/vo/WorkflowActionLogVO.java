package com.hrpm.vo;

import com.hrpm.entity.WorkflowActionLogRow;
import java.time.Instant;

public record WorkflowActionLogVO(String id, String taskId, Integer nodeNo, String actorUserId,
                                  String actorUsername, String action, String comment, Instant createdTime) {
    public static WorkflowActionLogVO from(WorkflowActionLogRow row) {
        return new WorkflowActionLogVO(Long.toString(row.id()), row.taskId() == null ? null : Long.toString(row.taskId()),
                row.nodeNo(), Long.toString(row.actorUserId()), row.actorUsername(), row.action(), row.comment(), row.createdTime());
    }
}
