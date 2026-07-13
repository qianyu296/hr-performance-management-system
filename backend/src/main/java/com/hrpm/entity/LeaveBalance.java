package com.hrpm.entity;

import java.math.BigDecimal;

import com.hrpm.common.exception.InsufficientLeaveBalanceException;

public record LeaveBalance(long id, long employeeId, String balanceType, int balanceYear,
                           BigDecimal availableHours, BigDecimal frozenHours, int version) {
    public LeaveBalance {
        if (id <= 0 || employeeId <= 0 || balanceType == null || balanceType.isBlank()
                || availableHours == null || availableHours.signum() < 0
                || frozenHours == null || frozenHours.signum() < 0 || version < 0) {
            throw new IllegalArgumentException("Invalid leave balance");
        }
    }

    public LeaveBalance(long id, long employeeId, String balanceType, BigDecimal availableHours, int version) {
        this(id, employeeId, balanceType, 0, availableHours, BigDecimal.ZERO, version);
    }

    public LeaveBalance changeBy(BigDecimal deltaHours) {
        BigDecimal nextHours = availableHours.add(deltaHours);
        if (nextHours.signum() < 0) {
            throw new InsufficientLeaveBalanceException();
        }
        return new LeaveBalance(id, employeeId, balanceType, balanceYear, nextHours, frozenHours, version + 1);
    }
}
