package com.hrpm.service;

import com.hrpm.entity.WorkCalendar;
import com.hrpm.entity.WorkCalendarDay;
import com.hrpm.mapper.WorkCalendarMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WorkTimeService {
    private static final BigDecimal DEFAULT_WORK_HOURS = BigDecimal.valueOf(8);
    private final WorkCalendarMapper workCalendarMapper;

    public WorkTimeService(WorkCalendarMapper workCalendarMapper) {
        this.workCalendarMapper = workCalendarMapper;
    }

    public BigDecimal calculateWorkHours(Instant startTime, Instant endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Leave end time must be after start time");
        }
        LocalDate startDate = startTime.atOffset(ZoneOffset.UTC).toLocalDate();
        LocalDate endDate = endTime.atOffset(ZoneOffset.UTC).toLocalDate();
        Map<LocalDate, WorkCalendarDay> configuredDays = configuredDays(startDate, endDate);
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            BigDecimal hours = workHours(date, configuredDays.get(date));
            if (hours.signum() == 0) {
                continue;
            }
            LocalDateTime workStart = date.atTime(9, 0);
            LocalDateTime workEnd = workStart.plusMinutes(hours.multiply(BigDecimal.valueOf(60)).longValueExact());
            Instant segmentStart = workStart.toInstant(ZoneOffset.UTC);
            Instant segmentEnd = workEnd.toInstant(ZoneOffset.UTC);
            Instant overlapStart = startTime.isAfter(segmentStart) ? startTime : segmentStart;
            Instant overlapEnd = endTime.isBefore(segmentEnd) ? endTime : segmentEnd;
            if (overlapEnd.isAfter(overlapStart)) {
                total = total.add(BigDecimal.valueOf(ChronoUnit.MINUTES.between(overlapStart, overlapEnd))
                        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<LocalDate, WorkCalendarDay> configuredDays(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, WorkCalendarDay> result = new HashMap<>();
        for (int year = startDate.getYear(); year <= endDate.getYear(); year++) {
            WorkCalendar calendar = workCalendarMapper.findActiveByYear(year);
            if (calendar == null) {
                continue;
            }
            LocalDate rangeStart = year == startDate.getYear() ? startDate : LocalDate.of(year, 1, 1);
            LocalDate rangeEnd = year == endDate.getYear() ? endDate : LocalDate.of(year, 12, 31);
            for (WorkCalendarDay day : workCalendarMapper.findDaysBetween(calendar.id(), rangeStart, rangeEnd)) {
                result.put(day.workDate(), day);
            }
        }
        return result;
    }

    private BigDecimal workHours(LocalDate date, WorkCalendarDay configuredDay) {
        if (configuredDay != null) {
            return configuredDay.workday() ? configuredDay.workHours() : BigDecimal.ZERO;
        }
        return switch (date.getDayOfWeek()) {
            case SATURDAY, SUNDAY -> BigDecimal.ZERO;
            default -> DEFAULT_WORK_HOURS;
        };
    }
}
