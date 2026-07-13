package com.hrpm.entity;

import java.util.List;

public record WorkflowInstanceSnapshot(
        long templateId,
        int templateVersion,
        long initiatorEmployeeId,
        long departmentId,
        List<WorkflowNodeSnapshot> nodes) {
}
