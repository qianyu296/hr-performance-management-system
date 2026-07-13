package com.hrpm.entity;

public record WorkflowBusinessContext(long instanceId, String businessType, long businessId, long actorUserId) {
}
