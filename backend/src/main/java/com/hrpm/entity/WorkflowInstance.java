package com.hrpm.entity;

public record WorkflowInstance(long id, String businessType, long businessId, long initiatorUserId,
                               String status, Integer currentNodeNo, int version) {
}
