package com.hrpm.entity;

import java.util.List;

public record WorkflowTemplateConfig(
        WorkflowTemplateDefinition template,
        List<Long> departmentIds,
        List<WorkflowTemplateNode> nodes) {
}
