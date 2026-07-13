package com.hrpm.mapper;

import com.hrpm.entity.LeaveBalance;
import com.hrpm.entity.LeaveBalanceChange;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LeaveBalanceMapper {
    @Select("SELECT id, employee_id AS employeeId, balance_type AS balanceType, balance_year AS balanceYear, available_hours AS availableHours, frozen_hours AS frozenHours, version FROM att_leave_balance WHERE employee_id = #{employeeId} AND deleted = 0 ORDER BY balance_year DESC, balance_type")
    List<LeaveBalance> listByEmployeeId(@Param("employeeId") long employeeId);

    @Select("SELECT id, employee_id AS employeeId, balance_type AS balanceType, balance_year AS balanceYear, available_hours AS availableHours, frozen_hours AS frozenHours, version FROM att_leave_balance WHERE id = #{id} AND deleted = 0")
    LeaveBalance findById(@Param("id") long id);

    @Update("UPDATE att_leave_balance SET available_hours = #{availableHours}, version = version + 1 WHERE id = #{id} AND version = #{version} AND deleted = 0")
    int updateAvailableHours(@Param("id") long id, @Param("version") int version, @Param("availableHours") BigDecimal availableHours);

    @Insert("INSERT INTO att_balance_change (id, balance_id, employee_id, balance_type, delta_hours, before_hours, after_hours, source_type, source_id, reason, created_by) VALUES (#{id}, #{balanceId}, #{employeeId}, #{balanceType}, #{deltaHours}, #{beforeHours}, #{afterHours}, 'MANUAL_ADJUSTMENT', #{sourceId}, #{reason}, #{createdBy})")
    int insertManualAdjustment(@Param("id") long id, @Param("balanceId") long balanceId, @Param("employeeId") long employeeId,
                               @Param("balanceType") String balanceType, @Param("deltaHours") BigDecimal deltaHours,
                               @Param("beforeHours") BigDecimal beforeHours, @Param("afterHours") BigDecimal afterHours,
                               @Param("sourceId") long sourceId, @Param("reason") String reason, @Param("createdBy") long createdBy);

    @Select("SELECT id, balance_id AS balanceId, balance_type AS balanceType, delta_hours AS deltaHours, before_hours AS beforeHours, after_hours AS afterHours, source_type AS sourceType, reason, created_by AS createdBy, created_time AS createdTime FROM att_balance_change WHERE balance_id = #{balanceId} ORDER BY created_time DESC, id DESC")
    List<LeaveBalanceChange> listChanges(@Param("balanceId") long balanceId);
}
