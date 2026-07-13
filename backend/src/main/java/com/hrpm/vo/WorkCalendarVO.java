package com.hrpm.vo;

import com.hrpm.entity.WorkCalendar;
import java.util.List;

public record WorkCalendarVO(String id, int calendarYear, String name, String timeZone, String status,
                             String version, List<WorkCalendarDayVO> days) {
    public static WorkCalendarVO from(WorkCalendar calendar, List<WorkCalendarDayVO> days) {
        return new WorkCalendarVO(Long.toString(calendar.id()), calendar.calendarYear(), calendar.name(), calendar.timeZone(),
                calendar.status(), Integer.toString(calendar.version()), days);
    }
}
