package com.hrpm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubmitLeaveRequestDTO(@NotNull @Min(0) Integer version) {
}
