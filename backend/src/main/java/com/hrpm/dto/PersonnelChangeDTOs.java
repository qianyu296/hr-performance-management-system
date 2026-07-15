package com.hrpm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public final class PersonnelChangeDTOs {
    private PersonnelChangeDTOs() {
    }

    public record ChangeAssignment(String employeeNo, String name, String gender,
                                   String departmentId, String positionId, String rankId,
                                   String managerEmployeeId, String employmentStatus,
                                   LocalDate hireDate, LocalDate probationStartDate,
                                   LocalDate probationEndDate, LocalDate terminationDate) {
    }

    public record CreateChange(String employeeId, @NotBlank String changeType,
                               @NotNull LocalDate effectiveDate, @NotBlank String reason,
                               @NotNull @Valid ChangeAssignment afterAssignment) {
    }

    public record UpdateChange(String employeeId, @NotBlank String changeType,
                               @NotNull LocalDate effectiveDate, @NotBlank String reason,
                               @NotNull @Valid ChangeAssignment afterAssignment,
                               @NotBlank String version) {
    }

    public record ChangeAction(@NotBlank String version) {
    }

    public record CreateHandoverItem(@NotBlank String itemType, String receiverEmployeeId,
                                     Boolean required, String remark) {
    }

    public record ConfirmHandoverItem(@NotBlank String version, String remark) {
    }
}
