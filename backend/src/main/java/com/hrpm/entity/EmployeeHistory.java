package com.hrpm.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeHistory(long id, long employeeId, Long changeId, String eventType,
                              LocalDate effectiveDate, String snapshot, Long createdBy,
                              LocalDateTime createdTime) {
}
