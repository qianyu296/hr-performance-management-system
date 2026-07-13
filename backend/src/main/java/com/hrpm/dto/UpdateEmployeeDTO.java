package com.hrpm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdateEmployeeDTO(@NotBlank String name, String gender,
                                @NotBlank String departmentId, @NotBlank String positionId,
                                String rankId, String managerEmployeeId,
                                @NotNull LocalDate hireDate, LocalDate probationStartDate,
                                LocalDate probationEndDate, @NotBlank String version) {
}
