package com.hrpm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRankDTO(@NotBlank String code, @NotBlank String name,
                            @NotNull Integer rankOrder, @NotBlank String status) {
}
