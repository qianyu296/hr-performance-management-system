package com.hrpm.entity;

import java.math.BigDecimal;
import java.time.Instant;

public record LeaveRequestRecord(
        long id,
        String requestNo,
        long employeeId,
        long leaveTypeId,
        Instant startTime,
        Instant endTime,
        BigDecimal durationHours,
        String reason,
        String status,
        String organizationSnapshot) {
}
