package com.hrpm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateOvertimeRequestDTO(@NotNull Instant startTime, @NotNull Instant endTime,
                                       @NotBlank String reason, @NotBlank String compensationType) {
}
