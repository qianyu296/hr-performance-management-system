package com.hrpm.entity;

public record WorkflowTemplateDefinition(
        long id,
        String code,
        String name,
        String businessType,
        int priority,
        int templateVersion,
        String status,
        int version) {
}
