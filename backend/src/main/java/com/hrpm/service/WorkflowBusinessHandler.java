package com.hrpm.service;

import com.hrpm.entity.WorkflowBusinessContext;

public interface WorkflowBusinessHandler {
    String businessType();

    void approve(WorkflowBusinessContext context);

    void reject(WorkflowBusinessContext context);

    void withdraw(WorkflowBusinessContext context);

    void returnToDraft(WorkflowBusinessContext context);
}
