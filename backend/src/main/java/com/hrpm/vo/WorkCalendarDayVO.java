package com.hrpm.vo;

import com.hrpm.entity.WorkCalendarDay;
import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkCalendarDayVO(String id, LocalDate workDate, boolean workday, BigDecimal workHours,
                                String holidayName, String version) {
    public static WorkCalendarDayVO from(WorkCalendarDay day) {
        return new WorkCalendarDayVO(Long.toString(day.id()), day.workDate(), day.workday(), day.workHours(),
                day.holidayName(), Integer.toString(day.version()));
    }
}
