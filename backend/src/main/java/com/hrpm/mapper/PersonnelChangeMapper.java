package com.hrpm.mapper;

import com.hrpm.entity.EmployeeHistory;
import com.hrpm.entity.ExitHandover;
import com.hrpm.entity.ExitHandoverItem;
import com.hrpm.entity.PersonnelChange;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PersonnelChangeMapper {
    String CHANGE_COLUMNS = """
            id, change_no AS changeNo, employee_id AS employeeId, change_type AS changeType,
            application_date AS applicationDate, effective_date AS effectiveDate, reason,
            before_snapshot AS beforeSnapshot, after_snapshot AS afterSnapshot,
            workflow_instance_id AS workflowInstanceId, status, created_by AS createdBy,
            created_time AS createdTime, version
            """;

    String HISTORY_COLUMNS = """
            id, employee_id AS employeeId, change_id AS changeId, event_type AS eventType,
            effective_date AS effectiveDate, snapshot, created_by AS createdBy,
            created_time AS createdTime
            """;

    String HANDOVER_COLUMNS = """
            id, change_id AS changeId, handover_employee_id AS handoverEmployeeId, status, version
            """;

    String HANDOVER_ITEM_COLUMNS = """
            id, handover_id AS handoverId, item_type AS itemType, receiver_employee_id AS receiverEmployeeId,
            is_required AS required, status, completed_time AS completedTime, confirmed_by AS confirmedBy,
            remark, version
            """;

    @Select("SELECT " + CHANGE_COLUMNS + " FROM hr_personnel_change WHERE id = #{id} AND deleted = 0")
    PersonnelChange findById(@Param("id") long id);

    @Select("SELECT " + CHANGE_COLUMNS + " FROM hr_personnel_change WHERE workflow_instance_id = #{workflowInstanceId} AND deleted = 0")
    PersonnelChange findByWorkflowInstanceId(@Param("workflowInstanceId") long workflowInstanceId);

    @Insert("""
            INSERT INTO hr_personnel_change (
                id, change_no, employee_id, change_type, application_date, effective_date, reason,
                before_snapshot, after_snapshot, workflow_instance_id, status, created_by, updated_by
            ) VALUES (
                #{id}, #{changeNo}, #{employeeId}, #{changeType}, #{applicationDate}, #{effectiveDate}, #{reason},
                CAST(#{beforeSnapshot} AS JSON), CAST(#{afterSnapshot} AS JSON), #{workflowInstanceId}, #{status}, #{createdBy}, #{updatedBy}
            )
            """)
    int insertChange(@Param("id") long id, @Param("changeNo") String changeNo, @Param("employeeId") Long employeeId,
                     @Param("changeType") String changeType, @Param("applicationDate") java.time.LocalDate applicationDate,
                     @Param("effectiveDate") java.time.LocalDate effectiveDate, @Param("reason") String reason,
                     @Param("beforeSnapshot") String beforeSnapshot, @Param("afterSnapshot") String afterSnapshot,
                     @Param("workflowInstanceId") Long workflowInstanceId, @Param("status") String status,
                     @Param("createdBy") Long createdBy, @Param("updatedBy") Long updatedBy);

    @Update("""
            UPDATE hr_personnel_change
            SET employee_id = #{employeeId},
                change_type = #{changeType},
                effective_date = #{effectiveDate},
                reason = #{reason},
                before_snapshot = CAST(#{beforeSnapshot} AS JSON),
                after_snapshot = CAST(#{afterSnapshot} AS JSON),
                updated_by = #{updatedBy},
                version = version + 1
            WHERE id = #{id}
              AND status = 'DRAFT'
              AND version = #{version}
              AND deleted = 0
            """)
    int updateDraft(@Param("id") long id, @Param("employeeId") Long employeeId, @Param("changeType") String changeType,
                    @Param("effectiveDate") java.time.LocalDate effectiveDate, @Param("reason") String reason,
                    @Param("beforeSnapshot") String beforeSnapshot, @Param("afterSnapshot") String afterSnapshot,
                    @Param("updatedBy") Long updatedBy, @Param("version") int version);

    @Update("""
            UPDATE hr_personnel_change
            SET status = #{newStatus},
                workflow_instance_id = COALESCE(#{workflowInstanceId}, workflow_instance_id),
                updated_by = #{updatedBy},
                version = version + 1
            WHERE id = #{id}
              AND status = #{expectedStatus}
              AND version = #{version}
              AND deleted = 0
            """)
    int updateStatus(@Param("id") long id, @Param("expectedStatus") String expectedStatus, @Param("newStatus") String newStatus,
                     @Param("workflowInstanceId") Long workflowInstanceId, @Param("updatedBy") Long updatedBy,
                     @Param("version") int version);

    @Update("""
            UPDATE hr_personnel_change
            SET status = 'EFFECTIVE',
                employee_id = COALESCE(#{employeeId}, employee_id),
                updated_by = #{updatedBy},
                version = version + 1
            WHERE id = #{id}
              AND status = 'APPROVED'
              AND version = #{version}
              AND deleted = 0
            """)
    int markEffective(@Param("id") long id, @Param("employeeId") Long employeeId,
                      @Param("updatedBy") Long updatedBy, @Param("version") int version);

    @Insert("""
            INSERT INTO hr_employee_history (id, employee_id, change_id, event_type, effective_date, snapshot, created_by)
            VALUES (#{id}, #{employeeId}, #{changeId}, #{eventType}, #{effectiveDate}, CAST(#{snapshot} AS JSON), #{createdBy})
            """)
    int insertHistory(EmployeeHistory history);

    @Select("SELECT " + HISTORY_COLUMNS + " FROM hr_employee_history WHERE employee_id = #{employeeId} ORDER BY effective_date DESC, id DESC")
    List<EmployeeHistory> listEmployeeHistory(@Param("employeeId") long employeeId);

    @Select("""
            <script>
            SELECT """ + CHANGE_COLUMNS + """
            FROM hr_personnel_change
            WHERE deleted = 0
            <if test='employeeId != null'>AND employee_id = #{employeeId}</if>
            <if test='changeType != null and changeType != ""'>AND change_type = #{changeType}</if>
            <if test='status != null and status != ""'>AND status = #{status}</if>
            <if test='fromDate != null'>AND effective_date &gt;= #{fromDate}</if>
            <if test='toDate != null'>AND effective_date &lt;= #{toDate}</if>
            <if test='unrestricted == false'>
              AND (
                (employee_id IS NOT NULL AND (
                  <if test='scopeEmployeeIds != null and scopeEmployeeIds.size() &gt; 0'>employee_id IN <foreach collection='scopeEmployeeIds' item='scopeId' open='(' separator=',' close=')'>#{scopeId}</foreach></if>
                  <if test='scopeEmployeeIds != null and scopeEmployeeIds.size() &gt; 0 and scopeDepartmentIds != null and scopeDepartmentIds.size() &gt; 0'> OR </if>
                  <if test='scopeDepartmentIds != null and scopeDepartmentIds.size() &gt; 0'>employee_id IN (
                    SELECT id FROM hr_employee
                    WHERE deleted = 0 AND department_id IN <foreach collection='scopeDepartmentIds' item='scopeId' open='(' separator=',' close=')'>#{scopeId}</foreach>
                  )</if>
                )))
                OR (employee_id IS NULL AND created_by = #{userId})
              )
            </if>
            ORDER BY created_time DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<PersonnelChange> findPage(@Param("userId") long userId, @Param("employeeId") Long employeeId,
                                   @Param("changeType") String changeType, @Param("status") String status,
                                   @Param("fromDate") java.time.LocalDate fromDate, @Param("toDate") java.time.LocalDate toDate,
                                   @Param("unrestricted") boolean unrestricted,
                                   @Param("scopeEmployeeIds") List<Long> scopeEmployeeIds,
                                   @Param("scopeDepartmentIds") List<Long> scopeDepartmentIds,
                                   @Param("offset") int offset, @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM hr_personnel_change
            WHERE deleted = 0
            <if test='employeeId != null'>AND employee_id = #{employeeId}</if>
            <if test='changeType != null and changeType != ""'>AND change_type = #{changeType}</if>
            <if test='status != null and status != ""'>AND status = #{status}</if>
            <if test='fromDate != null'>AND effective_date &gt;= #{fromDate}</if>
            <if test='toDate != null'>AND effective_date &lt;= #{toDate}</if>
            <if test='unrestricted == false'>
              AND (
                (employee_id IS NOT NULL AND (
                  <if test='scopeEmployeeIds != null and scopeEmployeeIds.size() &gt; 0'>employee_id IN <foreach collection='scopeEmployeeIds' item='scopeId' open='(' separator=',' close=')'>#{scopeId}</foreach></if>
                  <if test='scopeEmployeeIds != null and scopeEmployeeIds.size() &gt; 0 and scopeDepartmentIds != null and scopeDepartmentIds.size() &gt; 0'> OR </if>
                  <if test='scopeDepartmentIds != null and scopeDepartmentIds.size() &gt; 0'>employee_id IN (
                    SELECT id FROM hr_employee
                    WHERE deleted = 0 AND department_id IN <foreach collection='scopeDepartmentIds' item='scopeId' open='(' separator=',' close=')'>#{scopeId}</foreach>
                  )</if>
                )))
                OR (employee_id IS NULL AND created_by = #{userId})
              )
            </if>
            </script>
            """)
    long count(@Param("userId") long userId, @Param("employeeId") Long employeeId,
               @Param("changeType") String changeType, @Param("status") String status,
               @Param("fromDate") java.time.LocalDate fromDate, @Param("toDate") java.time.LocalDate toDate,
               @Param("unrestricted") boolean unrestricted,
               @Param("scopeEmployeeIds") List<Long> scopeEmployeeIds,
               @Param("scopeDepartmentIds") List<Long> scopeDepartmentIds);

    @Insert("""
            INSERT INTO hr_exit_handover (id, change_id, handover_employee_id, status, created_by, updated_by)
            VALUES (#{id}, #{changeId}, #{handoverEmployeeId}, #{status}, #{createdBy}, #{updatedBy})
            """)
    int insertExitHandover(@Param("id") long id, @Param("changeId") long changeId,
                           @Param("handoverEmployeeId") long handoverEmployeeId, @Param("status") String status,
                           @Param("createdBy") Long createdBy, @Param("updatedBy") Long updatedBy);

    @Select("SELECT " + HANDOVER_COLUMNS + " FROM hr_exit_handover WHERE change_id = #{changeId} AND deleted = 0")
    ExitHandover findExitHandoverByChangeId(@Param("changeId") long changeId);

    @Select("SELECT " + HANDOVER_COLUMNS + " FROM hr_exit_handover WHERE id = #{id} AND deleted = 0")
    ExitHandover findExitHandoverById(@Param("id") long id);

    @Insert("""
            INSERT INTO hr_exit_handover_item (
                id, handover_id, item_type, receiver_employee_id, is_required, status, completed_time,
                confirmed_by, remark, created_by, updated_by
            ) VALUES (
                #{id}, #{handoverId}, #{itemType}, #{receiverEmployeeId}, #{required}, #{status}, #{completedTime},
                #{confirmedBy}, #{remark}, #{createdBy}, #{updatedBy}
            )
            """)
    int insertExitHandoverItem(@Param("id") long id, @Param("handoverId") long handoverId, @Param("itemType") String itemType,
                               @Param("receiverEmployeeId") Long receiverEmployeeId, @Param("required") boolean required,
                               @Param("status") String status, @Param("completedTime") java.time.LocalDateTime completedTime,
                               @Param("confirmedBy") Long confirmedBy, @Param("remark") String remark,
                               @Param("createdBy") Long createdBy, @Param("updatedBy") Long updatedBy);

    @Select("SELECT " + HANDOVER_ITEM_COLUMNS + " FROM hr_exit_handover_item WHERE handover_id = #{handoverId} AND deleted = 0 ORDER BY id")
    List<ExitHandoverItem> listExitHandoverItems(@Param("handoverId") long handoverId);

    @Select("SELECT " + HANDOVER_ITEM_COLUMNS + " FROM hr_exit_handover_item WHERE id = #{id} AND deleted = 0")
    ExitHandoverItem findExitHandoverItemById(@Param("id") long id);

    @Update("""
            UPDATE hr_exit_handover_item
            SET status = 'CONFIRMED',
                completed_time = CURRENT_TIMESTAMP(3),
                confirmed_by = #{confirmedBy},
                remark = #{remark},
                version = version + 1
            WHERE id = #{id}
              AND version = #{version}
              AND status = 'PENDING'
              AND deleted = 0
            """)
    int confirmExitHandoverItem(@Param("id") long id, @Param("confirmedBy") long confirmedBy,
                                @Param("remark") String remark, @Param("version") int version);

    @Update("""
            UPDATE hr_exit_handover
            SET status = #{status},
                updated_by = #{updatedBy},
                version = version + 1
            WHERE id = #{id}
              AND deleted = 0
            """)
    int updateExitHandoverStatus(@Param("id") long id, @Param("status") String status, @Param("updatedBy") long updatedBy);

    @Select("""
            SELECT COUNT(*)
            FROM hr_exit_handover_item
            WHERE handover_id = #{handoverId}
              AND is_required = 1
              AND status <> 'CONFIRMED'
              AND deleted = 0
            """)
    int countPendingRequiredExitHandoverItems(@Param("handoverId") long handoverId);
}
