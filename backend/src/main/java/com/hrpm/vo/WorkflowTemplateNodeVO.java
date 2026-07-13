package com.hrpm.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.hrpm.entity.WorkflowTemplateNode;

public record WorkflowTemplateNodeVO(String nodeNo, String nodeType, JsonNode approverRule) {
    public static WorkflowTemplateNodeVO from(WorkflowTemplateNode node, JsonNode approverRule) {
        return new WorkflowTemplateNodeVO(Integer.toString(node.nodeNo()), node.nodeType(), approverRule);
    }
}
