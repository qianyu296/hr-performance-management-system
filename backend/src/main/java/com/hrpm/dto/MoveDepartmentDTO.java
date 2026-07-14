package com.hrpm.dto;

import jakarta.validation.constraints.NotBlank;

public record MoveDepartmentDTO(String parentId, @NotBlank String version) {
}
