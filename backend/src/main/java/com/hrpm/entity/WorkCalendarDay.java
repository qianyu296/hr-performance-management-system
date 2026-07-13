package com.hrpm.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkCalendarDay(long id, long calendarId, LocalDate workDate, boolean workday,
                              BigDecimal workHours, String holidayName, int version) {
}
