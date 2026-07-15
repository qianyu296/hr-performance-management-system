package com.hrpm.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;

@JsonDeserialize(using = UpdateEmployeeDTODeserializer.class)
public record UpdateEmployeeDTO(@NotBlank String name, String gender, @NotBlank String version) {
}
