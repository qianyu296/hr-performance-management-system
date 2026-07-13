package com.hrpm.dto;

import jakarta.validation.constraints.NotBlank;

public record RebuildAttendanceMonthlySummaryDTO(@NotBlank String month) {
}
