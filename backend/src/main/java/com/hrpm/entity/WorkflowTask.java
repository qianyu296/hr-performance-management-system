package com.hrpm.entity;

public record WorkflowTask(long id, long instanceId, int nodeNo, long assigneeUserId, String status, int version, long businessId) {
}
