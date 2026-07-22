package com.hrpm.common.exception;

public class InsufficientLeaveBalanceException extends RuntimeException {
    public InsufficientLeaveBalanceException() {
        super("可用请假余额不足");
    }
}
