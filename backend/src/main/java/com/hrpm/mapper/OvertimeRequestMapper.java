package com.hrpm.mapper;

import com.hrpm.entity.LeaveBalanceRow;
import com.hrpm.entity.OvertimeRequestListRow;
import com.hrpm.entity.OvertimeRequestRecord;
import com.hrpm.entity.OvertimeRequestSubmission;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OvertimeRequestMapper {
    @Insert("""
            INSERT INTO att_overtime_request (id, request_no, employee_id, start_time, end_time, duration_hours,
                reason, compensation_type, status, organization_snapshot)
            VALUES (#{id}, #{requestNo}, #{employeeId}, #{startTime}, #{endTime}, #{durationHours}, #{reason},
                #{compensationType}, #{status}, #{organizationSnapshot})
            """)
    int insert(OvertimeRequestRecord request);

    @Select("""
            SELECT r.id, r.employee_id AS employeeId, e.department_id AS departmentId, e.employment_status AS employmentStatus,
                   r.start_time AS startTime, r.end_time AS endTime, r.duration_hours AS durationHours,
                   r.compensation_type AS compensationType, r.status, r.workflow_instance_id AS workflowInstanceId, r.version
            FROM att_overtime_request r
            JOIN hr_employee e ON e.id = r.employee_id AND e.deleted = 0
            WHERE r.id = #{id} AND r.deleted = 0
            """)
    OvertimeRequestSubmission findSubmission(@Param("id") long id);

    @Select("""
            SELECT id, request_no AS requestNo, start_time AS startTime, end_time AS endTime,
                   duration_hours AS durationHours, compensation_type AS compensationType, status,
                   workflow_instance_id AS workflowInstanceId, version
            FROM att_overtime_request
            WHERE employee_id = #{employeeId} AND deleted = 0
            ORDER BY created_time DESC, id DESC
            """)
    List<OvertimeRequestListRow> listByEmployeeId(@Param("employeeId") long employeeId);

    @Update("UPDATE att_overtime_request SET status = 'IN_PROGRESS', workflow_instance_id = #{instanceId}, version = version + 1 WHERE id = #{id} AND employee_id = #{employeeId} AND status = 'DRAFT' AND version = #{version} AND deleted = 0")
    int markSubmitted(@Param("id") long id, @Param("employeeId") long employeeId, @Param("version") int version,
                      @Param("instanceId") long instanceId);

    @Update("UPDATE att_overtime_request SET status = 'APPROVED', version = version + 1 WHERE id = #{id} AND status = 'IN_PROGRESS' AND version = #{version} AND deleted = 0")
    int approveRequest(@Param("id") long id, @Param("version") int version);

    @Update("UPDATE att_overtime_request SET status = 'REJECTED', version = version + 1 WHERE id = #{id} AND status = 'IN_PROGRESS' AND version = #{version} AND deleted = 0")
    int rejectRequest(@Param("id") long id, @Param("version") int version);

    @Update("UPDATE att_overtime_request SET status = 'DRAFT', version = version + 1 WHERE id = #{id} AND status = 'IN_PROGRESS' AND version = #{version} AND deleted = 0")
    int returnRequestToDraft(@Param("id") long id, @Param("version") int version);

    @Update("UPDATE att_overtime_request SET status = 'CANCELLED', version = version + 1 WHERE id = #{id} AND employee_id = #{employeeId} AND status = 'APPROVED' AND version = #{version} AND deleted = 0")
    int cancelRequest(@Param("id") long id, @Param("employeeId") long employeeId, @Param("version") int version);

    @Select("SELECT id, available_hours AS availableHours, version FROM att_leave_balance WHERE employee_id = #{employeeId} AND balance_type = 'TIME_OFF' AND balance_year = #{year} AND deleted = 0")
    LeaveBalanceRow findTimeOffBalance(@Param("employeeId") long employeeId, @Param("year") int year);

    @Insert("INSERT INTO att_leave_balance (id, employee_id, balance_type, balance_year, available_hours, created_by) VALUES (#{id}, #{employeeId}, 'TIME_OFF', #{year}, 0, #{createdBy})")
    int insertTimeOffBalance(@Param("id") long id, @Param("employeeId") long employeeId, @Param("year") int year,
                             @Param("createdBy") long createdBy);

    @Update("UPDATE att_leave_balance SET available_hours = #{availableHours}, version = version + 1 WHERE id = #{id} AND version = #{version} AND deleted = 0")
    int updateBalance(@Param("id") long id, @Param("version") int version, @Param("availableHours") BigDecimal availableHours);

    @Insert("""
            INSERT INTO att_balance_change (id, balance_id, employee_id, balance_type, delta_hours, before_hours,
                after_hours, source_type, source_id, reason, created_by)
            VALUES (#{id}, #{balanceId}, #{employeeId}, 'TIME_OFF', #{deltaHours}, #{beforeHours}, #{afterHours},
                #{sourceType}, #{requestId}, #{reason}, #{createdBy})
            """)
    int insertBalanceChange(@Param("id") long id, @Param("balanceId") long balanceId, @Param("employeeId") long employeeId,
                            @Param("deltaHours") BigDecimal deltaHours, @Param("beforeHours") BigDecimal beforeHours,
                            @Param("afterHours") BigDecimal afterHours, @Param("sourceType") String sourceType,
                            @Param("requestId") long requestId, @Param("reason") String reason, @Param("createdBy") long createdBy);
}
