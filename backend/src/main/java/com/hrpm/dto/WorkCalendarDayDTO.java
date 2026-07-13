package com.hrpm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkCalendarDayDTO(@NotNull LocalDate workDate, @NotNull Boolean workday,
                                 @NotNull @DecimalMin("0.00") BigDecimal workHours, String holidayName) {
}
