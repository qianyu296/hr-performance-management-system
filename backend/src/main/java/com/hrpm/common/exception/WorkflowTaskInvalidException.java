package com.hrpm.common.exception;

public class WorkflowTaskInvalidException extends RuntimeException {
    public WorkflowTaskInvalidException() {
        super("工作流任务无效");
    }
}
