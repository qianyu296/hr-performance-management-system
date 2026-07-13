package com.hrpm.entity;

import java.util.List;

public record EmployeeDataScope(boolean unrestricted, List<Long> employeeIds, List<Long> departmentIds) {
    public boolean isEmpty() {
        return !unrestricted && employeeIds.isEmpty() && departmentIds.isEmpty();
    }
}
