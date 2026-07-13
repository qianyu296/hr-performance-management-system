package com.hrpm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdjustLeaveBalanceDTO(@NotNull @DecimalMin(value = "0.01") BigDecimal deltaHours,
                                    @NotBlank String direction, @NotBlank String reason, @NotBlank String version) {
}
