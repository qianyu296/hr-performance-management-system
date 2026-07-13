package com.hrpm.entity;

public record WorkflowTask(long id, long instanceId, int nodeNo, String nodeSnapshot, long assigneeUserId, String status, int version, long businessId) {
}
