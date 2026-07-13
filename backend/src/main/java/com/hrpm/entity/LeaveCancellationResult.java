package com.hrpm.entity;

public record LeaveCancellationResult(LeaveRequest request, LeaveBalance balance, BalanceChange BalanceChange) {
}
