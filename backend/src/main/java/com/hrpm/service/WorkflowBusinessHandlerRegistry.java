package com.hrpm.service;

import com.hrpm.common.exception.WorkflowTaskInvalidException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WorkflowBusinessHandlerRegistry {
    private final Map<String, WorkflowBusinessHandler> handlers;

    public WorkflowBusinessHandlerRegistry(List<WorkflowBusinessHandler> handlers) {
        this.handlers = new HashMap<>();
        for (WorkflowBusinessHandler handler : handlers) {
            if (this.handlers.put(handler.businessType(), handler) != null) {
                throw new IllegalStateException("Duplicate workflow business handler: " + handler.businessType());
            }
        }
    }

    public WorkflowBusinessHandler require(String businessType) {
        WorkflowBusinessHandler handler = handlers.get(businessType);
        if (handler == null) {
            throw new WorkflowTaskInvalidException();
        }
        return handler;
    }
}
