package com.hrpm.vo;

import com.hrpm.entity.Employee;
import java.time.LocalDate;

public record EmployeeVO(String id, String employeeNo, String name, String gender,
                         String departmentId, String departmentName,
                         String positionId, String positionName,
                         String rankId, String rankName,
                         String managerEmployeeId, String managerName,
                         String employmentStatus, LocalDate hireDate,
                         LocalDate probationStartDate, LocalDate probationEndDate,
                         String version) {
    public static EmployeeVO from(Employee value) {
        return new EmployeeVO(Long.toString(value.id()), value.employeeNo(), value.name(), value.gender(),
                Long.toString(value.departmentId()), value.departmentName(), Long.toString(value.positionId()),
                value.positionName(), value.rankId() == null ? null : Long.toString(value.rankId()), value.rankName(),
                value.managerEmployeeId() == null ? null : Long.toString(value.managerEmployeeId()), value.managerName(),
                value.employmentStatus(), value.hireDate(), value.probationStartDate(), value.probationEndDate(),
                Integer.toString(value.version()));
    }
}
