package com.hrpm.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PersonnelChange(long id, String changeNo, Long employeeId, String changeType,
                              LocalDate applicationDate, LocalDate effectiveDate, String reason,
                              String beforeSnapshot, String afterSnapshot, Long workflowInstanceId,
                              String status, Long createdBy, LocalDateTime createdTime, int version) {
}
