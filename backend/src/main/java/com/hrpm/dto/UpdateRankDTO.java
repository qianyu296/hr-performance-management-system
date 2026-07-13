package com.hrpm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateRankDTO(@NotBlank String name, @NotNull Integer rankOrder,
                            @NotBlank String status, @NotBlank String version) {
}
