package com.hrpm.service;

import com.hrpm.entity.Department;
import com.hrpm.entity.Employee;
import com.hrpm.entity.EmployeeDataScope;
import com.hrpm.entity.RoleDataScope;
import com.hrpm.entity.UserAccount;
import com.hrpm.mapper.DataScopeMapper;
import com.hrpm.mapper.DepartmentMapper;
import com.hrpm.mapper.EmployeeMapper;
import com.hrpm.mapper.UserAccountMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class EmployeeDataScopeResolver {
    private final DataScopeMapper dataScopeMapper;
    private final UserAccountMapper userAccountMapper;
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;

    public EmployeeDataScopeResolver(DataScopeMapper dataScopeMapper, UserAccountMapper userAccountMapper,
            EmployeeMapper employeeMapper, DepartmentMapper departmentMapper) {
        this.dataScopeMapper = dataScopeMapper;
        this.userAccountMapper = userAccountMapper;
        this.employeeMapper = employeeMapper;
        this.departmentMapper = departmentMapper;
    }

    public EmployeeDataScope resolve(long userId) {
        List<RoleDataScope> scopes = dataScopeMapper.findScopesByUserId(userId);
        if (scopes.stream().anyMatch(scope -> "ALL".equals(scope.scopeType()))) {
            return new EmployeeDataScope(true, List.of(), List.of());
        }
        UserAccount account = userAccountMapper.findById(userId);
        Long employeeId = account == null ? null : account.employeeId();
        Set<Long> employees = new LinkedHashSet<>();
        Set<Long> departments = new LinkedHashSet<>(dataScopeMapper.findCustomDepartmentIdsByUserId(userId));
        if (employeeId != null) {
            Employee currentEmployee = employeeMapper.findById(employeeId);
            for (RoleDataScope scope : scopes) {
                switch (scope.scopeType()) {
                    case "SELF" -> employees.add(employeeId);
                    case "DIRECT" -> employees.addAll(employeeMapper.findDirectReportIds(employeeId));
                    case "DEPT_TREE" -> addDepartmentTree(departments, currentEmployee);
                    case "DEPT" -> { if (currentEmployee != null) departments.add(currentEmployee.departmentId()); }
                    default -> { }
                }
            }
        }
        return new EmployeeDataScope(false, List.copyOf(employees), List.copyOf(departments));
    }

    private void addDepartmentTree(Set<Long> departments, Employee employee) {
        if (employee == null) return;
        Department department = departmentMapper.findById(employee.departmentId());
        if (department == null) return;
        for (Department candidate : departmentMapper.findAll()) {
            if (candidate.path().startsWith(department.path())) departments.add(candidate.id());
        }
    }
}
