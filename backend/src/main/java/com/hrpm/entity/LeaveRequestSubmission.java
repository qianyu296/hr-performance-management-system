package com.hrpm.entity;

import java.math.BigDecimal;
import java.time.Instant;

public record LeaveRequestSubmission(
        long id,
        long employeeId,
        long departmentId,
        String employmentStatus,
        long leaveTypeId,
        String balanceType,
        boolean deductBalance,
        String leaveTypeStatus,
        Instant startTime,
        Instant endTime,
        BigDecimal durationHours,
        String status,
        Long workflowInstanceId,
        int version) {
}
