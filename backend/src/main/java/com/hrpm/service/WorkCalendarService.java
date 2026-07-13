package com.hrpm.service;

import com.hrpm.common.IdGenerator;
import com.hrpm.common.exception.DuplicateResourceException;
import com.hrpm.common.exception.OrganizationReferenceInvalidException;
import com.hrpm.common.exception.ResourceNotFoundException;
import com.hrpm.common.exception.VersionConflictException;
import com.hrpm.dto.CreateWorkCalendarDTO;
import com.hrpm.dto.UpdateWorkCalendarDTO;
import com.hrpm.dto.WorkCalendarDayDTO;
import com.hrpm.entity.WorkCalendar;
import com.hrpm.mapper.WorkCalendarMapper;
import com.hrpm.vo.WorkCalendarDayVO;
import com.hrpm.vo.WorkCalendarVO;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkCalendarService {
    private final WorkCalendarMapper mapper;
    private final IdGenerator idGenerator;

    public WorkCalendarService(WorkCalendarMapper mapper, IdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    public WorkCalendarVO getByYear(int year) {
        WorkCalendar calendar = mapper.findByYear(year);
        if (calendar == null) {
            throw new ResourceNotFoundException("Work calendar not found");
        }
        return view(calendar);
    }

    @Transactional
    public WorkCalendarVO create(CreateWorkCalendarDTO request) {
        validate(request.calendarYear(), request.timeZone(), request.status(), request.days());
        if (mapper.findByYear(request.calendarYear()) != null) {
            throw new DuplicateResourceException("Work calendar year already exists");
        }
        long id = idGenerator.nextId();
        mapper.insertCalendar(id, request.calendarYear(), request.name(), request.timeZone(), request.status());
        insertDays(id, request.days());
        return getByYear(request.calendarYear());
    }

    @Transactional
    public WorkCalendarVO update(long id, UpdateWorkCalendarDTO request) {
        WorkCalendar current = mapper.findById(id);
        if (current == null) {
            throw new ResourceNotFoundException("Work calendar not found");
        }
        validate(current.calendarYear(), request.timeZone(), request.status(), request.days());
        if (mapper.updateCalendar(id, request.name(), request.timeZone(), request.status(), parseVersion(request.version())) != 1) {
            throw new VersionConflictException();
        }
        mapper.deleteDays(id);
        insertDays(id, request.days());
        return view(mapper.findById(id));
    }

    private WorkCalendarVO view(WorkCalendar calendar) {
        return WorkCalendarVO.from(calendar, mapper.findDays(calendar.id()).stream().map(WorkCalendarDayVO::from).toList());
    }

    private void insertDays(long calendarId, List<WorkCalendarDayDTO> days) {
        for (WorkCalendarDayDTO day : days) {
            mapper.insertDay(idGenerator.nextId(), calendarId, day.workDate(), day.workday(), day.workHours(), day.holidayName());
        }
    }

    private void validate(int year, String timeZone, String status, List<WorkCalendarDayDTO> days) {
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new OrganizationReferenceInvalidException("Invalid work calendar status");
        }
        try {
            ZoneId.of(timeZone);
        } catch (DateTimeException exception) {
            throw new OrganizationReferenceInvalidException("Invalid work calendar time zone");
        }
        Set<java.time.LocalDate> dates = new HashSet<>();
        for (WorkCalendarDayDTO day : days) {
            if (day.workDate().getYear() != year || !dates.add(day.workDate())
                    || (!day.workday() && day.workHours().signum() != 0)
                    || (day.workday() && day.workHours().signum() <= 0)) {
                throw new OrganizationReferenceInvalidException("Invalid work calendar day configuration");
            }
        }
    }

    private int parseVersion(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new OrganizationReferenceInvalidException("Invalid work calendar version");
        }
    }
}
