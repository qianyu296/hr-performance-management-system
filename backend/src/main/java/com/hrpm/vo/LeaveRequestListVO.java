package com.hrpm.vo;

import java.math.BigDecimal;
import java.time.Instant;

public record LeaveRequestListVO(
        String id,
        String requestNo,
        String leaveTypeName,
        Instant startTime,
        Instant endTime,
        BigDecimal durationHours,
        String status,
        String workflowInstanceId,
        int version) {
}
