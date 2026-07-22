package com.hrpm.common.exception;

public class IllegalLeaveStateTransitionException extends RuntimeException {
    public IllegalLeaveStateTransitionException() {
        super("当前请假单状态不允许执行该操作");
    }
}
