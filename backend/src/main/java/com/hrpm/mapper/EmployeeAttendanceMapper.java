package com.hrpm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeAttendanceMapper {
    @Select("""
            SELECT employment_status
            FROM hr_employee
            WHERE id = #{id} AND deleted = 0
            """)
    String findEmploymentStatus(@Param("id") long id);
}
