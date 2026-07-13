package com.hrpm.mapper;


import com.hrpm.entity.LeaveBalanceRow;
import com.hrpm.entity.LeaveRequestListRow;
import com.hrpm.entity.LeaveRequestRecord;
import com.hrpm.entity.LeaveRequestSubmission;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Mapper
public interface LeaveRequestMapper {
    @Insert("""
            INSERT INTO att_leave_request (
                id, request_no, employee_id, leave_type_id, start_time, end_time,
                duration_hours, reason, status, organization_snapshot)
            VALUES (
                #{id}, #{requestNo}, #{employeeId}, #{leaveTypeId}, #{startTime}, #{endTime},
                #{durationHours}, #{reason}, #{status}, #{organizationSnapshot})
            """)
    int insert(LeaveRequestRecord request);

    @Select("""
            SELECT r.id, r.employee_id AS employeeId, e.department_id AS departmentId, e.employment_status AS employmentStatus,
                   r.leave_type_id AS leaveTypeId, t.code AS balanceType, t.deduct_balance AS deductBalance,
                   t.status AS leaveTypeStatus, r.start_time AS startTime, r.end_time AS endTime,
                   r.duration_hours AS durationHours, r.status, r.workflow_instance_id AS workflowInstanceId, r.version
            FROM att_leave_request r
            JOIN hr_employee e ON e.id = r.employee_id AND e.deleted = 0
            JOIN att_leave_type t ON t.id = r.leave_type_id AND t.deleted = 0
            WHERE r.id = #{id} AND r.deleted = 0
            """)
    LeaveRequestSubmission findSubmission(@Param("id") long id);

    @Select("""
            SELECT r.id, r.request_no AS requestNo, t.name AS leaveTypeName, r.start_time AS startTime,
                   r.end_time AS endTime, r.duration_hours AS durationHours, r.status,
                   r.workflow_instance_id AS workflowInstanceId, r.version
            FROM att_leave_request r
            JOIN att_leave_type t ON t.id = r.leave_type_id AND t.deleted = 0
            WHERE r.employee_id = #{employeeId} AND r.deleted = 0
            ORDER BY r.created_time DESC, r.id DESC
            """)
    List<LeaveRequestListRow> listByEmployeeId(@Param("employeeId") long employeeId);

    @Select("""
            SELECT COUNT(*) FROM att_leave_request
            WHERE employee_id = #{employeeId} AND id <> #{requestId} AND deleted = 0
              AND status IN ('DRAFT', 'IN_PROGRESS', 'APPROVED')
              AND start_time < #{endTime} AND end_time > #{startTime}
            """)
    int countOverlaps(@Param("employeeId") long employeeId, @Param("requestId") long requestId,
            @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Select("""
            SELECT available_hours FROM att_leave_balance
            WHERE employee_id = #{employeeId} AND balance_type = #{balanceType} AND balance_year = #{year} AND deleted = 0
            """)
    BigDecimal findAvailableBalance(@Param("employeeId") long employeeId, @Param("balanceType") String balanceType,
            @Param("year") int year);

    @Update("""
            UPDATE att_leave_request SET status = 'IN_PROGRESS', workflow_instance_id = #{instanceId}, version = version + 1
            WHERE id = #{id} AND employee_id = #{employeeId} AND status = 'DRAFT' AND version = #{version} AND deleted = 0
            """)
    int markSubmitted(@Param("id") long id, @Param("employeeId") long employeeId,
            @Param("version") int version, @Param("instanceId") long instanceId);

    @Select("SELECT id, available_hours AS availableHours, version FROM att_leave_balance WHERE employee_id = #{employeeId} AND balance_type = #{balanceType} AND balance_year = #{year} AND deleted = 0")
    LeaveBalanceRow findBalance(@Param("employeeId") long employeeId, @Param("balanceType") String balanceType, @Param("year") int year);

    @Update("UPDATE att_leave_request SET status = 'APPROVED', version = version + 1 WHERE id = #{id} AND status = 'IN_PROGRESS' AND version = #{version}")
    int approveRequest(@Param("id") long id, @Param("version") int version);

    @Update("UPDATE att_leave_request SET status = 'REJECTED', version = version + 1 WHERE id = #{id} AND status = 'IN_PROGRESS' AND version = #{version}")
    int rejectRequest(@Param("id") long id, @Param("version") int version);

    @Update("UPDATE att_leave_request SET status = 'DRAFT', version = version + 1 WHERE id = #{id} AND status = 'IN_PROGRESS' AND version = #{version}")
    int returnRequestToDraft(@Param("id") long id, @Param("version") int version);

    @Update("UPDATE att_leave_balance SET available_hours = #{availableHours}, version = version + 1 WHERE id = #{id} AND version = #{version}")
    int updateBalance(@Param("id") long id, @Param("version") int version, @Param("availableHours") BigDecimal availableHours);

    @Insert("""
            INSERT INTO att_balance_change (id, balance_id, employee_id, balance_type, delta_hours, before_hours, after_hours, source_type, source_id, reason)
            VALUES (#{id}, #{balanceId}, #{employeeId}, #{balanceType}, #{deltaHours}, #{beforeHours}, #{afterHours}, 'LEAVE_APPROVAL', #{requestId}, 'Leave request approved')
            """)
    int insertBalanceChange(@Param("id") long id, @Param("balanceId") long balanceId, @Param("employeeId") long employeeId,
            @Param("balanceType") String balanceType, @Param("deltaHours") BigDecimal deltaHours,
            @Param("beforeHours") BigDecimal beforeHours, @Param("afterHours") BigDecimal afterHours, @Param("requestId") long requestId);

    @Update("UPDATE att_leave_request SET status = 'CANCELLED', version = version + 1 WHERE id = #{id} AND employee_id = #{employeeId} AND status = 'APPROVED' AND version = #{version}")
    int cancelRequest(@Param("id") long id, @Param("employeeId") long employeeId, @Param("version") int version);

    @Insert("""
            INSERT INTO att_balance_change (id, balance_id, employee_id, balance_type, delta_hours, before_hours, after_hours, source_type, source_id, reason)
            VALUES (#{id}, #{balanceId}, #{employeeId}, #{balanceType}, #{deltaHours}, #{beforeHours}, #{afterHours}, 'LEAVE_CANCELLATION', #{requestId}, 'Leave request cancelled')
            """)
    int insertCancellationBalanceChange(@Param("id") long id, @Param("balanceId") long balanceId, @Param("employeeId") long employeeId,
            @Param("balanceType") String balanceType, @Param("deltaHours") BigDecimal deltaHours,
            @Param("beforeHours") BigDecimal beforeHours, @Param("afterHours") BigDecimal afterHours, @Param("requestId") long requestId);
}
