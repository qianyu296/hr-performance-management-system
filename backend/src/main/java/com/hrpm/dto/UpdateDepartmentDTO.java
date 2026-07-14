package com.hrpm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record UpdateDepartmentDTO(
        @NotBlank String name,
        String leaderEmployeeId,
        @NotNull @Min(0) Integer sortNo,
        @NotBlank String status,
        @NotNull LocalDate effectiveDate,
        @NotBlank String version) {
}
