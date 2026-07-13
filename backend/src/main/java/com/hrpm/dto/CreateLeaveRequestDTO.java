package com.hrpm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateLeaveRequestDTO(
        @NotBlank String leaveTypeId,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @NotBlank String reason) {
}
