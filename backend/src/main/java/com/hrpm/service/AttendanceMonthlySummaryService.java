package com.hrpm.service;

import com.hrpm.common.IdGenerator;
import com.hrpm.dto.RebuildAttendanceMonthlySummaryDTO;
import com.hrpm.entity.AttendanceMonthlySummary;
import com.hrpm.mapper.AttendanceMonthlySummaryMapper;
import com.hrpm.vo.AttendanceMonthlySummaryVO;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceMonthlySummaryService {
    private final AttendanceMonthlySummaryMapper mapper;
    private final IdGenerator idGenerator;

    public AttendanceMonthlySummaryService(AttendanceMonthlySummaryMapper mapper, IdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public int rebuild(long userId, RebuildAttendanceMonthlySummaryDTO request) {
        YearMonth month = parseMonth(request.month());
        return mapper.rebuild(idGenerator.nextId(), month.atDay(1), month.plusMonths(1).atDay(1), userId);
    }

    public List<AttendanceMonthlySummaryVO> list(String monthValue, Long departmentId, Long employeeId) {
        YearMonth month = parseMonth(monthValue);
        return mapper.list(month.atDay(1), departmentId, employeeId).stream().map(AttendanceMonthlySummaryVO::from).toList();
    }

    private YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Month must use yyyy-MM format");
        }
    }
}
