package com.hrpm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateEmployeeDTO(@NotBlank String employeeNo, @NotBlank String name, String gender,
                                @NotBlank String departmentId, @NotBlank String positionId,
                                String rankId, String managerEmployeeId,
                                @NotBlank String employmentStatus, @NotNull LocalDate hireDate,
                                LocalDate probationStartDate, LocalDate probationEndDate) {
}
