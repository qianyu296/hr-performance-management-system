package com.hrpm.mapper;


import com.hrpm.entity.Department;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface DepartmentMapper {
    @Select("""
            SELECT id, code, name, parent_id AS parentId, leader_employee_id AS leaderEmployeeId,
                   path, sort_no AS sortNo, effective_date AS effectiveDate, status, version
            FROM hr_department
            WHERE deleted = 0
            ORDER BY path, sort_no, id
            """)
    List<Department> findAll();

    @Select("""
            SELECT id, code, name, parent_id AS parentId, leader_employee_id AS leaderEmployeeId,
                   path, sort_no AS sortNo, effective_date AS effectiveDate, status, version
            FROM hr_department
            WHERE id = #{id} AND deleted = 0
            """)
    Department findById(@Param("id") long id);

    @Select("SELECT COUNT(*) FROM hr_department WHERE code = #{code} AND deleted = 0")
    int countByCode(@Param("code") String code);

    @Insert("""
            INSERT INTO hr_department (id, code, name, parent_id, leader_employee_id, path, sort_no, effective_date, status)
            VALUES (#{id}, #{code}, #{name}, #{parentId}, #{leaderEmployeeId}, #{path}, #{sortNo}, #{effectiveDate}, #{status})
            """)
    int insert(Department department);

    @Update("""
            UPDATE hr_department
            SET name = #{name}, leader_employee_id = #{leaderEmployeeId}, sort_no = #{sortNo},
                effective_date = #{effectiveDate}, status = #{status}, version = version + 1
            WHERE id = #{id} AND version = #{version} AND deleted = 0
            """)
    int update(Department department);

    @Update("""
            UPDATE hr_department
            SET parent_id = #{parentId}, path = #{path}, version = version + 1
            WHERE id = #{id} AND version = #{version} AND deleted = 0
            """)
    int move(Department department);

    @Update("""
            UPDATE hr_department
            SET path = CONCAT(#{newPath}, SUBSTRING(path, #{oldPathLength} + 1)), version = version + 1
            WHERE path LIKE CONCAT(#{oldPath}, '%') AND id <> #{departmentId} AND deleted = 0
            """)
    int replaceDescendantPathPrefix(@Param("departmentId") long departmentId, @Param("oldPath") String oldPath,
                                    @Param("newPath") String newPath, @Param("oldPathLength") int oldPathLength);

    @Select("SELECT COUNT(*) FROM hr_department WHERE parent_id = #{departmentId} AND status = 'ACTIVE' AND deleted = 0")
    int countActiveChildren(@Param("departmentId") long departmentId);

    @Select("SELECT COUNT(*) FROM hr_employee WHERE department_id = #{departmentId} AND employment_status <> 'TERMINATED' AND deleted = 0")
    int countActiveEmployees(@Param("departmentId") long departmentId);
}
