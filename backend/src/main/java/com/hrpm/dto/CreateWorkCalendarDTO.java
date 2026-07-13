package com.hrpm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateWorkCalendarDTO(@NotNull @Min(2000) @Max(2100) Integer calendarYear, @NotBlank String name,
                                    @NotBlank String timeZone, @NotBlank String status,
                                    @NotNull List<@Valid WorkCalendarDayDTO> days) {
}
