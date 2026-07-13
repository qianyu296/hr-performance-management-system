package com.hrpm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateLeaveTypeDTO(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 128) String name,
        boolean deductBalance,
        BigDecimal annualQuota,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal minUnitHours) {
}
