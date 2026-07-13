package com.hrpm.mapper;


import com.hrpm.entity.LeaveType;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface LeaveTypeMapper {
    @Select("""
            SELECT id, code, name, deduct_balance AS deductBalance, min_unit_hours AS minUnitHours, status
            FROM att_leave_type
            WHERE id = #{id} AND deleted = 0
            """)
    LeaveType findById(@Param("id") long id);

    @Select("""
            SELECT id, code, name, deduct_balance AS deductBalance, min_unit_hours AS minUnitHours, status
            FROM att_leave_type
            WHERE status = 'ACTIVE' AND deleted = 0
            ORDER BY code
            """)
    List<LeaveType> listActive();
}
