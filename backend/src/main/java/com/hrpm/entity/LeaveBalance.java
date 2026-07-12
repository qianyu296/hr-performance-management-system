package com.hrpm.entity;


import com.hrpm.common.exception.InsufficientLeaveBalanceException;

import java.math.BigDecimal;

public record LeaveBalance(long id, long employeeId, String balanceType, BigDecimal availableHours, int version) {
    public LeaveBalance {
        if (id <= 0 || employeeId <= 0 || balanceType == null || balanceType.isBlank()
                || availableHours == null || availableHours.signum() < 0 || version < 0) {
            throw new IllegalArgumentException("Invalid leave balance");
        }
    }

    public LeaveBalance changeBy(BigDecimal deltaHours) {
        BigDecimal nextHours = availableHours.add(deltaHours);
        if (nextHours.signum() < 0) {
            throw new InsufficientLeaveBalanceException();
        }
        return new LeaveBalance(id, employeeId, balanceType, nextHours, version + 1);
    }
}
