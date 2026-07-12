package com.hrpm.common.exception;

public class IllegalLeaveStateTransitionException extends RuntimeException {
    public IllegalLeaveStateTransitionException() {
        super("Leave request state does not allow this action");
    }
}
