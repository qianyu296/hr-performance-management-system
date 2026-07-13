package com.hrpm.service;


import com.hrpm.common.exception.DepartmentNotFoundException;
import com.hrpm.common.IdGenerator;
import com.hrpm.dto.CreateDepartmentDTO;
import com.hrpm.entity.Department;
import com.hrpm.mapper.DepartmentMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hrpm.vo.DepartmentVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DepartmentService {
    private final DepartmentMapper departmentMapper;
    private final IdGenerator idGenerator;

    public DepartmentService(DepartmentMapper departmentMapper, IdGenerator idGenerator) {
        this.departmentMapper = departmentMapper;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public List<DepartmentVO> listTree() {
        Map<Long, DepartmentVO> nodes = new LinkedHashMap<>();
        for (Department department : departmentMapper.findAll()) {
            nodes.put(department.id(), DepartmentVO.from(department));
        }
        List<DepartmentVO> roots = new ArrayList<>();
        for (DepartmentVO node : nodes.values()) {
            if (node.parentId() == null) {
                roots.add(node);
                continue;
            }
            DepartmentVO parent = nodes.get(Long.parseLong(node.parentId()));
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        return roots;
    }

    @Transactional
    public Department create(CreateDepartmentDTO request) {
        if (departmentMapper.countByCode(request.code()) > 0) {
            throw new IllegalArgumentException("Department code already exists");
        }
        Long parentId = request.parentId() == null || request.parentId().isBlank() ? null : Long.parseLong(request.parentId());
        Department parent = parentId == null ? null : departmentMapper.findById(parentId);
        if (parentId != null && (parent == null || !"ACTIVE".equals(parent.status()))) {
            throw new DepartmentNotFoundException();
        }
        long id = idGenerator.nextId();
        Department department = new Department(
                id,
                request.code(),
                request.name(),
                parentId,
                parent == null ? "/" + id + "/" : parent.path() + id + "/",
                request.effectiveDate(),
                request.status());
        departmentMapper.insert(department);
        return department;
    }
}
