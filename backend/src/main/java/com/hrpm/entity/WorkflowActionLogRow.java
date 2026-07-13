package com.hrpm.entity;

import java.time.Instant;

public record WorkflowActionLogRow(long id, Long taskId, Integer nodeNo, long actorUserId,
                                   String actorUsername, String action, String comment, Instant createdTime) {
}
