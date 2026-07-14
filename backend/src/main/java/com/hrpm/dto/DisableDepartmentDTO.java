package com.hrpm.dto;

import jakarta.validation.constraints.NotBlank;

public record DisableDepartmentDTO(@NotBlank String version) {
}
