package com.hrpm.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateLeaveTypeDTO(@NotBlank String name, boolean deductBalance, BigDecimal annualQuota,
                                 @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal minUnitHours,
                                 @NotBlank String version) {
}
