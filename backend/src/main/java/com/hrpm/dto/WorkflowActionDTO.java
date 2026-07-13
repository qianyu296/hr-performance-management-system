package com.hrpm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkflowActionDTO(@NotNull @Min(0) Integer version, @NotBlank String comment) {
}
