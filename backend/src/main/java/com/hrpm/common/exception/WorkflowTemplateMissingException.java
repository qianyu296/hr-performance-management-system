package com.hrpm.common.exception;

public class WorkflowTemplateMissingException extends RuntimeException {
    public WorkflowTemplateMissingException() {
        super("No matching workflow template is available");
    }
}
