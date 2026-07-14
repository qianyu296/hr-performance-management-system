package com.hrpm.service;

import com.hrpm.entity.Department;
import com.hrpm.entity.EmployeeDataScope;
import com.hrpm.entity.Employee;
import com.hrpm.common.exception.DataScopeDeniedException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OrganizationAccessService {
    private final EmployeeDataScopeResolver dataScopeResolver;

    public OrganizationAccessService(EmployeeDataScopeResolver dataScopeResolver) {
        this.dataScopeResolver = dataScopeResolver;
    }

    public List<Department> visibleDepartments(long userId, List<Department> allDepartments) {
        EmployeeDataScope scope = dataScopeResolver.resolve(userId);
        if (scope.unrestricted()) return allDepartments;

        Set<Long> visibleIds = new LinkedHashSet<>(scope.departmentIds());
        for (Department department : allDepartments) {
            if (scope.departmentIds().contains(department.id())) {
                String[] parts = department.path().split("/");
                for (String part : parts) {
                    if (!part.isBlank()) visibleIds.add(Long.parseLong(part));
                }
            }
        }
        return allDepartments.stream().filter(department -> visibleIds.contains(department.id())).toList();
    }

    public void requireWritableDepartment(long userId, long departmentId) {
        requireWritableDepartments(userId, List.of(departmentId));
    }

    public void requireWritableDepartments(long userId, List<Long> departmentIds) {
        EmployeeDataScope scope = dataScopeResolver.resolve(userId);
        if (!scope.unrestricted() && departmentIds.stream().anyMatch(id -> !scope.departmentIds().contains(id))) {
            throw new DataScopeDeniedException();
        }
    }

    public void requireWritableEmployee(long userId, Employee employee) {
        EmployeeDataScope scope = dataScopeResolver.resolve(userId);
        if (!scope.unrestricted()
                && !scope.employeeIds().contains(employee.id())
                && !scope.departmentIds().contains(employee.departmentId())) {
            throw new DataScopeDeniedException();
        }
    }

    public void requireWritableNewDepartment(long userId, Long parentId) {
        if (parentId == null) {
            EmployeeDataScope scope = dataScopeResolver.resolve(userId);
            if (!scope.unrestricted()) throw new DataScopeDeniedException();
            return;
        }
        requireWritableDepartment(userId, parentId);
    }
}
