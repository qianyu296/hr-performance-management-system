package com.hrpm.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AttendanceMonthlySummary(long id, long employeeId, String employeeNo, String employeeName,
                                       long departmentId, String departmentName, LocalDate attendanceMonth,
                                       BigDecimal leaveHours, BigDecimal overtimeHours, BigDecimal timeOffDeltaHours,
                                       int pendingRequestCount, Long generatedBy, Instant generatedTime) {
}
