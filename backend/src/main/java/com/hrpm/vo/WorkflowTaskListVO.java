package com.hrpm.vo;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
public record WorkflowTaskListVO(
        String id,
        String instanceId,
        String businessType,
        String businessId,
        String requestNo,
        String applicantName,
        String leaveTypeName,
        Instant startTime,
        Instant endTime,
        LocalDate effectiveDate,
        BigDecimal durationHours,
        String status,
        int version) {
}