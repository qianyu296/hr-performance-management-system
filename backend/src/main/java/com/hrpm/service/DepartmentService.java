package com.hrpm.service;

import com.hrpm.common.IdGenerator;
import com.hrpm.common.exception.DepartmentNotFoundException;
import com.hrpm.common.exception.DuplicateResourceException;
import com.hrpm.common.exception.OrganizationReferenceInvalidException;
import com.hrpm.common.exception.VersionConflictException;
import com.hrpm.dto.CreateDepartmentDTO;
import com.hrpm.dto.DisableDepartmentDTO;
import com.hrpm.dto.MoveDepartmentDTO;
import com.hrpm.dto.UpdateDepartmentDTO;
import com.hrpm.entity.Department;
import com.hrpm.entity.Employee;
import com.hrpm.mapper.DepartmentMapper;
import com.hrpm.mapper.EmployeeMapper;
import com.hrpm.vo.DepartmentVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {
    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;
    private final OrganizationAccessService organizationAccessService;
    private final IdGenerator idGenerator;

    public DepartmentService(DepartmentMapper departmentMapper, EmployeeMapper employeeMapper,
                             OrganizationAccessService organizationAccessService, IdGenerator idGenerator) {
        this.departmentMapper = departmentMapper;
        this.employeeMapper = employeeMapper;
        this.organizationAccessService = organizationAccessService;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public List<DepartmentVO> listTree(long userId) {
        Map<Long, DepartmentVO> nodes = new LinkedHashMap<>();
        for (Department department : organizationAccessService.visibleDepartments(userId, departmentMapper.findAll())) {
            nodes.put(department.id(), DepartmentVO.from(department));
        }
        List<DepartmentVO> roots = new ArrayList<>();
        for (DepartmentVO node : nodes.values()) {
            if (node.parentId() == null) {
                roots.add(node);
                continue;
            }
            DepartmentVO parent = nodes.get(Long.parseLong(node.parentId()));
            if (parent == null) roots.add(node);
            else parent.children().add(node);
        }
        return roots;
    }

    @Transactional
    public Department create(long userId, CreateDepartmentDTO request) {
        Long parentId = parseNullable(request.parentId());
        organizationAccessService.requireWritableNewDepartment(userId, parentId);
        if (departmentMapper.countByCode(request.code()) > 0) {
            throw new DuplicateResourceException("Department code already exists");
        }
        Department parent = parentId == null ? null : requireActiveParent(parentId);
        validateStatus(request.status());
        long id = idGenerator.nextId();
        Long leaderEmployeeId = parseNullable(request.leaderEmployeeId());
        validateLeader(userId, leaderEmployeeId);
        Department department = new Department(
                id, request.code(), request.name(), parentId, leaderEmployeeId,
                parent == null ? "/" + id + "/" : parent.path() + id + "/",
                request.sortNo() == null ? 0 : request.sortNo(), request.effectiveDate(), request.status(), 0);
        departmentMapper.insert(department);
        return department;
    }

    @Transactional
    public Department update(long userId, long id, UpdateDepartmentDTO request) {
        organizationAccessService.requireWritableDepartment(userId, id);
        Department current = departmentMapper.findById(id);
        if (current == null) throw new VersionConflictException();
        validateStatus(request.status());
        Long leaderEmployeeId = parseNullable(request.leaderEmployeeId());
        validateLeader(userId, leaderEmployeeId);
        if ("ACTIVE".equals(request.status()) && current.parentId() != null) {
            organizationAccessService.requireWritableDepartment(userId, current.parentId());
            requireActiveParent(current.parentId());
        }
        if (!"ACTIVE".equals(request.status())) ensureDisableAllowed(id);
        Department updated = new Department(id, current.code(), request.name(), current.parentId(), leaderEmployeeId,
                current.path(), request.sortNo(), request.effectiveDate(), request.status(), parseVersion(request.version()));
        if (departmentMapper.update(updated) == 0) throw new VersionConflictException();
        return departmentMapper.findById(id);
    }

    @Transactional
    public Department move(long userId, long id, MoveDepartmentDTO request) {
        organizationAccessService.requireWritableDepartment(userId, id);
        Department current = departmentMapper.findById(id);
        if (current == null) throw new VersionConflictException();
        Long parentId = parseNullable(request.parentId());
        if (parentId != null && parentId == id) {
            throw new OrganizationReferenceInvalidException("Department cannot be its own parent");
        }
        organizationAccessService.requireWritableNewDepartment(userId, parentId);
        Department parent = parentId == null ? null : requireActiveParent(parentId);
        if (parent != null && parent.path().startsWith(current.path())) {
            throw new OrganizationReferenceInvalidException("Department cannot move under its descendant");
        }
        List<Long> affectedDepartmentIds = departmentMapper.findAll().stream()
                .filter(department -> department.path().startsWith(current.path()))
                .map(Department::id)
                .toList();
        organizationAccessService.requireWritableDepartments(userId, affectedDepartmentIds);
        String targetPath = parent == null ? "/" + id + "/" : parent.path() + id + "/";
        Department moved = new Department(id, current.code(), current.name(), parentId, current.leaderEmployeeId(),
                targetPath, current.sortNo(), current.effectiveDate(), current.status(), parseVersion(request.version()));
        if (departmentMapper.move(moved) == 0) throw new VersionConflictException();
        departmentMapper.replaceDescendantPathPrefix(id, current.path(), targetPath, current.path().length());
        return departmentMapper.findById(id);
    }

    @Transactional
    public Department disable(long userId, long id, DisableDepartmentDTO request) {
        organizationAccessService.requireWritableDepartment(userId, id);
        Department current = departmentMapper.findById(id);
        if (current == null) throw new VersionConflictException();
        ensureDisableAllowed(id);
        Department disabled = new Department(id, current.code(), current.name(), current.parentId(), current.leaderEmployeeId(),
                current.path(), current.sortNo(), current.effectiveDate(), "INACTIVE", parseVersion(request.version()));
        if (departmentMapper.update(disabled) == 0) throw new VersionConflictException();
        return departmentMapper.findById(id);
    }

    private void ensureDisableAllowed(long departmentId) {
        if (departmentMapper.countActiveChildren(departmentId) > 0) {
            throw new IllegalStateException("Department has active child departments");
        }
        if (departmentMapper.countActiveEmployees(departmentId) > 0) {
            throw new IllegalStateException("Department has active employees");
        }
    }

    private void validateLeader(long userId, Long leaderEmployeeId) {
        if (leaderEmployeeId == null) return;
        Employee leader = employeeMapper.findById(leaderEmployeeId);
        if (leader == null || "TERMINATED".equals(leader.employmentStatus())) {
            throw new OrganizationReferenceInvalidException("Department leader is missing or inactive");
        }
        organizationAccessService.requireWritableEmployee(userId, leader);
    }

    private Department requireActiveParent(long parentId) {
        Department parent = departmentMapper.findById(parentId);
        if (parent == null) throw new DepartmentNotFoundException();
        if (!"ACTIVE".equals(parent.status())) {
            throw new OrganizationReferenceInvalidException("Parent department is inactive");
        }
        return parent;
    }

    private void validateStatus(String status) {
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new OrganizationReferenceInvalidException("Invalid department status");
        }
    }

    private Long parseNullable(String value) {
        return value == null || value.isBlank() ? null : Long.parseLong(value);
    }

    private int parseVersion(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new OrganizationReferenceInvalidException("Invalid department version");
        }
    }
}
