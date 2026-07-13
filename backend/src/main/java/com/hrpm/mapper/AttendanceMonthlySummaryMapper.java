package com.hrpm.mapper;

import com.hrpm.entity.AttendanceMonthlySummary;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AttendanceMonthlySummaryMapper {
    @Insert("""
            INSERT INTO rpt_attendance_month (id, employee_id, department_id, attendance_month, leave_hours,
                overtime_hours, time_off_delta_hours, pending_request_count, generated_by)
            SELECT #{idBase} + e.id, e.id, e.department_id, #{monthStart},
                   COALESCE(l.leave_hours, 0), COALESCE(o.overtime_hours, 0), COALESCE(b.time_off_delta_hours, 0),
                   COALESCE(p.pending_request_count, 0), #{generatedBy}
            FROM hr_employee e
            LEFT JOIN (
                SELECT employee_id, SUM(duration_hours) AS leave_hours
                FROM att_leave_request
                WHERE deleted = 0 AND status = 'APPROVED' AND start_time >= #{monthStart} AND start_time < #{nextMonthStart}
                GROUP BY employee_id
            ) l ON l.employee_id = e.id
            LEFT JOIN (
                SELECT employee_id, SUM(duration_hours) AS overtime_hours
                FROM att_overtime_request
                WHERE deleted = 0 AND status = 'APPROVED' AND start_time >= #{monthStart} AND start_time < #{nextMonthStart}
                GROUP BY employee_id
            ) o ON o.employee_id = e.id
            LEFT JOIN (
                SELECT employee_id, SUM(delta_hours) AS time_off_delta_hours
                FROM att_balance_change
                WHERE balance_type = 'TIME_OFF' AND created_time >= #{monthStart} AND created_time < #{nextMonthStart}
                GROUP BY employee_id
            ) b ON b.employee_id = e.id
            LEFT JOIN (
                SELECT employee_id, SUM(request_count) AS pending_request_count
                FROM (
                    SELECT employee_id, COUNT(*) AS request_count
                    FROM att_leave_request
                    WHERE deleted = 0 AND status IN ('DRAFT', 'IN_PROGRESS') AND start_time >= #{monthStart} AND start_time < #{nextMonthStart}
                    GROUP BY employee_id
                    UNION ALL
                    SELECT employee_id, COUNT(*) AS request_count
                    FROM att_overtime_request
                    WHERE deleted = 0 AND status IN ('DRAFT', 'IN_PROGRESS') AND start_time >= #{monthStart} AND start_time < #{nextMonthStart}
                    GROUP BY employee_id
                ) pending_requests
                GROUP BY employee_id
            ) p ON p.employee_id = e.id
            WHERE e.deleted = 0
            ON DUPLICATE KEY UPDATE department_id = VALUES(department_id), leave_hours = VALUES(leave_hours),
                overtime_hours = VALUES(overtime_hours), time_off_delta_hours = VALUES(time_off_delta_hours),
                pending_request_count = VALUES(pending_request_count), generated_by = VALUES(generated_by),
                generated_time = CURRENT_TIMESTAMP(3), version = rpt_attendance_month.version + 1
            """)
    int rebuild(@Param("idBase") long idBase, @Param("monthStart") LocalDate monthStart,
                @Param("nextMonthStart") LocalDate nextMonthStart, @Param("generatedBy") long generatedBy);

    @Select("""
            <script>
            SELECT r.id, r.employee_id AS employeeId, e.employee_no AS employeeNo, e.name AS employeeName,
                   r.department_id AS departmentId, d.name AS departmentName, r.attendance_month AS attendanceMonth,
                   r.leave_hours AS leaveHours, r.overtime_hours AS overtimeHours,
                   r.time_off_delta_hours AS timeOffDeltaHours, r.pending_request_count AS pendingRequestCount,
                   r.generated_by AS generatedBy, r.generated_time AS generatedTime
            FROM rpt_attendance_month r
            JOIN hr_employee e ON e.id = r.employee_id AND e.deleted = 0
            JOIN hr_department d ON d.id = r.department_id AND d.deleted = 0
            WHERE r.attendance_month = #{monthStart}
            <if test='departmentId != null'>AND r.department_id = #{departmentId}</if>
            <if test='employeeId != null'>AND r.employee_id = #{employeeId}</if>
            ORDER BY d.name, e.employee_no
            </script>
            """)
    List<AttendanceMonthlySummary> list(@Param("monthStart") LocalDate monthStart,
                                        @Param("departmentId") Long departmentId, @Param("employeeId") Long employeeId);
}
