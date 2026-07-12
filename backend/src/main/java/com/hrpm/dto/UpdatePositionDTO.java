package com.hrpm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePositionDTO(@NotBlank String name, String jobFamily, String description,
                                @NotNull Integer sortNo, @NotBlank String status, @NotBlank String version) {
}
