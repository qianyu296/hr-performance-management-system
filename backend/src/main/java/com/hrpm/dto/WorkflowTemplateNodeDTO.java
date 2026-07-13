package com.hrpm.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkflowTemplateNodeDTO(
        @NotNull @Min(1) Integer nodeNo,
        @NotBlank String nodeType,
        @NotNull JsonNode approverRule) {
}
