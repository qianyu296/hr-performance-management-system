package com.hrpm.common.exception;

public class WorkflowTemplateMissingException extends RuntimeException {
    public WorkflowTemplateMissingException() {
        super("未找到匹配的审批流程模板");
    }
}
