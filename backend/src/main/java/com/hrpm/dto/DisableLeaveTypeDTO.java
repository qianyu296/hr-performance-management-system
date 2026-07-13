package com.hrpm.dto;

import jakarta.validation.constraints.NotBlank;

public record DisableLeaveTypeDTO(@NotBlank String version) {
}
