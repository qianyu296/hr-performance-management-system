package com.hrpm.entity;

import java.time.LocalDate;

public record Employee(long id, String employeeNo, String name, String gender,
                       long departmentId, String departmentName,
                       long positionId, String positionName,
                       Long rankId, String rankName,
                       Long managerEmployeeId, String managerName,
                       String employmentStatus, LocalDate hireDate,
                       LocalDate probationStartDate, LocalDate probationEndDate,
                       int version) {
}
