package com.hrpm.entity;

import com.fasterxml.jackson.databind.JsonNode;

public record WorkflowNodeSnapshot(
        int nodeNo,
        String nodeType,
        JsonNode approverRule,
        long assigneeUserId) {
}
