package com.hrpm.common.exception;

public class WorkflowTaskInvalidException extends RuntimeException {
    public WorkflowTaskInvalidException() {
        super("Workflow task is invalid");
    }
}
