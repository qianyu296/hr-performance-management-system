package com.hrpm.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.hrpm.entity.WorkflowTemplateConfig;
import java.util.List;

public record WorkflowTemplateVO(
        String id,
        String code,
        String name,
        String businessType,
        int priority,
        String templateVersion,
        String status,
        String version,
        List<String> departmentIds,
        List<WorkflowTemplateNodeVO> nodes) {
    public static WorkflowTemplateVO from(WorkflowTemplateConfig config, java.util.function.Function<String, JsonNode> ruleParser) {
        var template = config.template();
        return new WorkflowTemplateVO(
                Long.toString(template.id()), template.code(), template.name(), template.businessType(), template.priority(),
                Integer.toString(template.templateVersion()), template.status(), Integer.toString(template.version()),
                config.departmentIds().stream().map(String::valueOf).toList(),
                config.nodes().stream().map(node -> WorkflowTemplateNodeVO.from(node, ruleParser.apply(node.approverRule()))).toList());
    }
}
