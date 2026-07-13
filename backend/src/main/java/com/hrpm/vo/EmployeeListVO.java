package com.hrpm.vo;

import com.hrpm.entity.Employee;

public record EmployeeListVO(String id, String employeeNo, String name, String departmentName,
                             String positionName, String rankName, String managerName,
                             String employmentStatus, String version) {
    public static EmployeeListVO from(Employee value) {
        return new EmployeeListVO(Long.toString(value.id()), value.employeeNo(), value.name(), value.departmentName(),
                value.positionName(), value.rankName(), value.managerName(), value.employmentStatus(),
                Integer.toString(value.version()));
    }
}
