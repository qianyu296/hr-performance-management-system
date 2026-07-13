package com.hrpm.mapper;


import com.hrpm.entity.Department;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DepartmentMapper {
    @Select("""
            SELECT id, code, name, parent_id AS parentId, path, effective_date AS effectiveDate, status
            FROM hr_department
            WHERE deleted = 0
            ORDER BY path, sort_no, id
            """)
    List<Department> findAll();

    @Select("""
            SELECT id, code, name, parent_id AS parentId, path, effective_date AS effectiveDate, status
            FROM hr_department
            WHERE id = #{id} AND deleted = 0
            """)
    Department findById(@Param("id") long id);

    @Select("SELECT COUNT(*) FROM hr_department WHERE code = #{code} AND deleted = 0")
    int countByCode(@Param("code") String code);

    @Insert("""
            INSERT INTO hr_department (id, code, name, parent_id, path, effective_date, status)
            VALUES (#{id}, #{code}, #{name}, #{parentId}, #{path}, #{effectiveDate}, #{status})
            """)
    int insert(Department department);
}
