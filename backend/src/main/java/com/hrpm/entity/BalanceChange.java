package com.hrpm.entity;

import java.math.BigDecimal;

public record BalanceChange(
        long balanceId,
        long leaveRequestId,
        BalanceChangeType type,
        BigDecimal deltaHours,
        BigDecimal beforeHours,
        BigDecimal afterHours) {
}
