package com.hrpm.entity;

import java.math.BigDecimal;
import java.time.Instant;

public record OvertimeRequestSubmission(long id, long employeeId, long departmentId, String employmentStatus,
                                        Instant startTime, Instant endTime, BigDecimal durationHours,
                                        String compensationType, String status, Long workflowInstanceId, int version) {
}
