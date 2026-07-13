package com.hrpm.entity;

import java.math.BigDecimal;
import java.time.Instant;

public record LeaveBalanceChange(long id, long balanceId, String balanceType, BigDecimal deltaHours,
                                 BigDecimal beforeHours, BigDecimal afterHours, String sourceType,
                                 String reason, Long createdBy, Instant createdTime) {
}
