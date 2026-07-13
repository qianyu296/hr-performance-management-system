package com.hrpm.vo;

import java.math.BigDecimal;
import java.time.Instant;

public record WorkflowTaskListVO(
        String id,
        String instanceId,
        String businessType,
        String businessId,
        String requestNo,
        String applicantName,
        String leaveTypeName,
        Instant startTime,
        Instant endTime,
        BigDecimal durationHours,
        String status,
        int version) {
}
