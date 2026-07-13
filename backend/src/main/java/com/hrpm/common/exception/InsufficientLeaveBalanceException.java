package com.hrpm.common.exception;

public class InsufficientLeaveBalanceException extends RuntimeException {
    public InsufficientLeaveBalanceException() {
        super("Leave balance is insufficient");
    }
}
