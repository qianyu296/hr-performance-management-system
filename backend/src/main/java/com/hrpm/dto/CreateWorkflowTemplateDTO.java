package com.hrpm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateWorkflowTemplateDTO(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String businessType,
        @NotNull @Min(0) Integer priority,
        @NotNull @Min(1) Integer templateVersion,
        @NotBlank String status,
        @NotNull List<@NotBlank String> departmentIds,
        @NotEmpty List<@Valid WorkflowTemplateNodeDTO> nodes) {
}
