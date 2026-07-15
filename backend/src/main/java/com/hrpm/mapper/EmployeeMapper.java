package com.hrpm.mapper;

import com.hrpm.entity.Employee;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface EmployeeMapper {
    String COLUMNS = """
            e.id, e.employee_no AS employeeNo, e.name, e.gender,
            e.department_id AS departmentId, d.name AS departmentName,
            e.position_id AS positionId, p.name AS positionName,
            e.rank_id AS rankId, r.name AS rankName,
            e.manager_employee_id AS managerEmployeeId, m.name AS managerName,
            e.employment_status AS employmentStatus, e.hire_date AS hireDate,
            e.probation_start_date AS probationStartDate, e.probation_end_date AS probationEndDate,
            e.version
            """;

    @Select("""
            <script>
            SELECT /* employee columns */ """ + COLUMNS + """
            FROM hr_employee e
            JOIN hr_department d ON d.id=e.department_id AND d.deleted=0
            JOIN hr_position p ON p.id=e.position_id AND p.deleted=0
            LEFT JOIN hr_rank r ON r.id=e.rank_id AND r.deleted=0
            LEFT JOIN hr_employee m ON m.id=e.manager_employee_id AND m.deleted=0
            WHERE e.deleted=0
            <if test='keyword != null and keyword != ""'>AND (e.employee_no LIKE CONCAT('%',#{keyword},'%') OR e.name LIKE CONCAT('%',#{keyword},'%'))</if>
            <if test='departmentId != null'>AND e.department_id=#{departmentId}</if>
            <if test='positionId != null'>AND e.position_id=#{positionId}</if>
            <if test='employmentStatus != null and employmentStatus != ""'>AND e.employment_status=#{employmentStatus}</if>
            <if test='scopeEmployeeIds != null and scopeEmployeeIds.size() > 0 or scopeDepartmentIds != null and scopeDepartmentIds.size() > 0'>AND (
              <if test='scopeEmployeeIds != null and scopeEmployeeIds.size() > 0'>e.id IN <foreach collection='scopeEmployeeIds' item='scopeId' open='(' separator=',' close=')'>#{scopeId}</foreach></if>
              <if test='scopeEmployeeIds != null and scopeEmployeeIds.size() > 0 and scopeDepartmentIds != null and scopeDepartmentIds.size() > 0'> OR </if>
              <if test='scopeDepartmentIds != null and scopeDepartmentIds.size() > 0'>e.department_id IN <foreach collection='scopeDepartmentIds' item='scopeId' open='(' separator=',' close=')'>#{scopeId}</foreach></if>
            )</if>
            ORDER BY e.employee_no LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Employee> findPage(@Param("keyword") String keyword, @Param("departmentId") Long departmentId,
                            @Param("positionId") Long positionId, @Param("employmentStatus") String employmentStatus,
                            @Param("scopeEmployeeIds") List<Long> scopeEmployeeIds, @Param("scopeDepartmentIds") List<Long> scopeDepartmentIds,
                            @Param("offset") int offset, @Param("limit") int limit);

    @Select("""
            <script>SELECT COUNT(*) FROM hr_employee e WHERE e.deleted=0
            <if test='keyword != null and keyword != ""'>AND (e.employee_no LIKE CONCAT('%',#{keyword},'%') OR e.name LIKE CONCAT('%',#{keyword},'%'))</if>
            <if test='departmentId != null'>AND e.department_id=#{departmentId}</if>
            <if test='positionId != null'>AND e.position_id=#{positionId}</if>
            <if test='employmentStatus != null and employmentStatus != ""'>AND e.employment_status=#{employmentStatus}</if>
            <if test='scopeEmployeeIds != null and scopeEmployeeIds.size() > 0 or scopeDepartmentIds != null and scopeDepartmentIds.size() > 0'>AND (
              <if test='scopeEmployeeIds != null and scopeEmployeeIds.size() > 0'>e.id IN <foreach collection='scopeEmployeeIds' item='scopeId' open='(' separator=',' close=')'>#{scopeId}</foreach></if>
              <if test='scopeEmployeeIds != null and scopeEmployeeIds.size() > 0 and scopeDepartmentIds != null and scopeDepartmentIds.size() > 0'> OR </if>
              <if test='scopeDepartmentIds != null and scopeDepartmentIds.size() > 0'>e.department_id IN <foreach collection='scopeDepartmentIds' item='scopeId' open='(' separator=',' close=')'>#{scopeId}</foreach></if>
            )</if>
            </script>
            """)
    long count(@Param("keyword") String keyword, @Param("departmentId") Long departmentId,
               @Param("positionId") Long positionId, @Param("employmentStatus") String employmentStatus,
               @Param("scopeEmployeeIds") List<Long> scopeEmployeeIds, @Param("scopeDepartmentIds") List<Long> scopeDepartmentIds);

    @Select("SELECT " + COLUMNS + " FROM hr_employee e JOIN hr_department d ON d.id=e.department_id AND d.deleted=0 JOIN hr_position p ON p.id=e.position_id AND p.deleted=0 LEFT JOIN hr_rank r ON r.id=e.rank_id AND r.deleted=0 LEFT JOIN hr_employee m ON m.id=e.manager_employee_id AND m.deleted=0 WHERE e.id=#{id} AND e.deleted=0")
    Employee findById(@Param("id") long id);

    @Select("SELECT id FROM hr_employee WHERE manager_employee_id=#{employeeId} AND deleted=0")
    List<Long> findDirectReportIds(@Param("employeeId") long employeeId);

    @Select("SELECT id FROM hr_employee WHERE deleted = 0 AND employment_status <> 'TERMINATED'")
    List<Long> listActiveIds();

    @Select("SELECT COUNT(*) FROM hr_employee WHERE employee_no=#{employeeNo} AND deleted=0")
    int countByEmployeeNo(@Param("employeeNo") String employeeNo);

    @Select("SELECT " + COLUMNS + " FROM hr_employee e JOIN hr_department d ON d.id=e.department_id AND d.deleted=0 JOIN hr_position p ON p.id=e.position_id AND p.deleted=0 LEFT JOIN hr_rank r ON r.id=e.rank_id AND r.deleted=0 LEFT JOIN hr_employee m ON m.id=e.manager_employee_id AND m.deleted=0 WHERE e.employee_no=#{employeeNo} AND e.deleted=0")
    Employee findByEmployeeNo(@Param("employeeNo") String employeeNo);

    @Insert("INSERT INTO hr_employee (id,employee_no,name,gender,department_id,position_id,rank_id,manager_employee_id,employment_status,hire_date,probation_start_date,probation_end_date) VALUES (#{id},#{employeeNo},#{name},#{gender},#{departmentId},#{positionId},#{rankId},#{managerEmployeeId},#{employmentStatus},#{hireDate},#{probationStartDate},#{probationEndDate})")
    int insert(Employee employee);

    @Update("UPDATE hr_employee SET name=#{name},gender=#{gender},version=version+1 WHERE id=#{id} AND version=#{version} AND deleted=0")
    int update(Employee employee);

    @Update("""
            UPDATE hr_employee
            SET employee_no = #{employeeNo},
                name = #{name},
                gender = #{gender},
                department_id = #{departmentId},
                position_id = #{positionId},
                rank_id = #{rankId},
                manager_employee_id = #{managerEmployeeId},
                employment_status = #{employmentStatus},
                hire_date = #{hireDate},
                probation_start_date = #{probationStartDate},
                probation_end_date = #{probationEndDate},
                termination_date = #{terminationDate},
                updated_by = #{updatedBy},
                version = version + 1
            WHERE id = #{id}
              AND version = #{version}
              AND deleted = 0
            """)
    int updateAssignment(@Param("id") long id, @Param("employeeNo") String employeeNo, @Param("name") String name,
                         @Param("gender") String gender, @Param("departmentId") long departmentId,
                         @Param("positionId") long positionId, @Param("rankId") Long rankId,
                         @Param("managerEmployeeId") Long managerEmployeeId,
                         @Param("employmentStatus") String employmentStatus,
                         @Param("hireDate") java.time.LocalDate hireDate,
                         @Param("probationStartDate") java.time.LocalDate probationStartDate,
                         @Param("probationEndDate") java.time.LocalDate probationEndDate,
                         @Param("terminationDate") java.time.LocalDate terminationDate,
                         @Param("updatedBy") Long updatedBy, @Param("version") int version);
}
