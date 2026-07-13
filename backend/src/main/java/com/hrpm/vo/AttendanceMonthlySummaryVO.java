package com.hrpm.vo;

import com.hrpm.entity.AttendanceMonthlySummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AttendanceMonthlySummaryVO(String id, String employeeId, String employeeNo, String employeeName,
                                         String departmentId, String departmentName, LocalDate attendanceMonth,
                                         BigDecimal leaveHours, BigDecimal overtimeHours, BigDecimal timeOffDeltaHours,
                                         int pendingRequestCount, String generatedBy, Instant generatedTime) {
    public static AttendanceMonthlySummaryVO from(AttendanceMonthlySummary summary) {
        return new AttendanceMonthlySummaryVO(Long.toString(summary.id()), Long.toString(summary.employeeId()),
                summary.employeeNo(), summary.employeeName(), Long.toString(summary.departmentId()), summary.departmentName(),
                summary.attendanceMonth(), summary.leaveHours(), summary.overtimeHours(), summary.timeOffDeltaHours(),
                summary.pendingRequestCount(), summary.generatedBy() == null ? null : Long.toString(summary.generatedBy()),
                summary.generatedTime());
    }
}
