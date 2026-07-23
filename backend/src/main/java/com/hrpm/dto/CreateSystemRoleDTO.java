package com.hrpm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateSystemRoleDTO(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String status,
        @NotBlank String dataScopeType,
        @NotNull List<@NotBlank String> menuIds,
        @NotNull List<@NotBlank String> departmentIds) {
}