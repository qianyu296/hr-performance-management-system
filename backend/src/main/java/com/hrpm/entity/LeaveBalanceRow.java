package com.hrpm.entity;

import java.math.BigDecimal;

public record LeaveBalanceRow(long id, BigDecimal availableHours, int version) {
}
