package com.hrpm.vo;


import com.hrpm.entity.Department;
import java.util.ArrayList;
import java.util.List;

public record DepartmentVO(
        String id,
        String code,
        String name,
        String parentId,
        String path,
        String status,
        List<DepartmentVO> children) {
    public static DepartmentVO from(Department department) {
        return new DepartmentVO(
                Long.toString(department.id()),
                department.code(),
                department.name(),
                department.parentId() == null ? null : Long.toString(department.parentId()),
                department.path(),
                department.status(),
                new ArrayList<>());
    }
}
