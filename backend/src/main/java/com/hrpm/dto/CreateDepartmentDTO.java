package com.hrpm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateDepartmentDTO(
        @NotBlank String code,
        @NotBlank String name,
        String parentId,
        @NotBlank String status,
        @NotNull LocalDate effectiveDate) {
}
