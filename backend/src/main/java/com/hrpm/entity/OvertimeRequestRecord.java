package com.hrpm.entity;

import java.math.BigDecimal;
import java.time.Instant;

public record OvertimeRequestRecord(long id, String requestNo, long employeeId, Instant startTime, Instant endTime,
                                    BigDecimal durationHours, String reason, String compensationType, String status,
                                    String organizationSnapshot) {
}
