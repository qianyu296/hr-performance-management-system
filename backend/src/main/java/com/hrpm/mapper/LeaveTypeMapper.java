package com.hrpm.mapper;


import com.hrpm.entity.LeaveType;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface LeaveTypeMapper {
    @Select("""
            SELECT id, code, name, deduct_balance AS deductBalance, min_unit_hours AS minUnitHours,
                   annual_quota AS annualQuota, status, version
            FROM att_leave_type
            WHERE id = #{id} AND deleted = 0
            """)
    LeaveType findById(@Param("id") long id);

    @Select("""
            SELECT id, code, name, deduct_balance AS deductBalance, min_unit_hours AS minUnitHours,
                   annual_quota AS annualQuota, status, version
            FROM att_leave_type
            WHERE status = 'ACTIVE' AND deleted = 0
            ORDER BY code
            """)
    List<LeaveType> listActive();

    @Select("SELECT id, code, name, deduct_balance AS deductBalance, min_unit_hours AS minUnitHours, annual_quota AS annualQuota, status, version FROM att_leave_type WHERE deleted = 0 ORDER BY code")
    List<LeaveType> listAll();

    @Select("SELECT id, code, name, deduct_balance AS deductBalance, min_unit_hours AS minUnitHours, annual_quota AS annualQuota, status, version FROM att_leave_type WHERE code = #{code} AND deleted = 0")
    LeaveType findByCode(@Param("code") String code);

    @Insert("INSERT INTO att_leave_type (id, code, name, deduct_balance, min_unit_hours, annual_quota, status) VALUES (#{id}, #{code}, #{name}, #{deductBalance}, #{minUnitHours}, #{annualQuota}, 'ACTIVE')")
    int insert(@Param("id") long id, @Param("code") String code, @Param("name") String name,
               @Param("deductBalance") boolean deductBalance, @Param("minUnitHours") java.math.BigDecimal minUnitHours,
               @Param("annualQuota") java.math.BigDecimal annualQuota);

    @Update("UPDATE att_leave_type SET name=#{name}, deduct_balance=#{deductBalance}, min_unit_hours=#{minUnitHours}, annual_quota=#{annualQuota}, version=version+1 WHERE id=#{id} AND version=#{version} AND deleted=0")
    int update(@Param("id") long id, @Param("name") String name, @Param("deductBalance") boolean deductBalance, @Param("minUnitHours") java.math.BigDecimal minUnitHours, @Param("annualQuota") java.math.BigDecimal annualQuota, @Param("version") int version);

    @Update("UPDATE att_leave_type SET status='INACTIVE', version=version+1 WHERE id=#{id} AND version=#{version} AND deleted=0")
    int disable(@Param("id") long id, @Param("version") int version);
}
