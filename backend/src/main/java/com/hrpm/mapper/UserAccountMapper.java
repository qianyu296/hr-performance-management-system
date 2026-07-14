package com.hrpm.mapper;


import com.hrpm.entity.UserAccount;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserAccountMapper {
    @Select("""
            SELECT id, username, password_hash AS passwordHash, employee_id AS employeeId, status, session_version AS sessionVersion,
                   password_change_required AS passwordChangeRequired
            FROM sys_user
            WHERE username = #{username} AND deleted = 0
            """)
    UserAccount findByUsername(@Param("username") String username);

    @Select("""
            SELECT id, username, password_hash AS passwordHash, employee_id AS employeeId, status, session_version AS sessionVersion,
                   password_change_required AS passwordChangeRequired
            FROM sys_user
            WHERE id = #{id} AND deleted = 0
            """)
    UserAccount findById(@Param("id") long id);

    @Insert("INSERT INTO sys_user (id, username, password_hash, employee_id, status, session_version, password_change_required) VALUES (#{id}, #{username}, #{passwordHash}, #{employeeId}, 'ACTIVE', 0, 1)")
    int insertEmployeeAccount(@Param("id") long id, @Param("username") String username, @Param("passwordHash") String passwordHash, @Param("employeeId") long employeeId);

    @Update("UPDATE sys_user SET password_hash=#{passwordHash}, password_change_required=0, session_version=session_version+1 WHERE id=#{id} AND session_version=#{sessionVersion} AND deleted=0")
    int changePassword(@Param("id") long id, @Param("passwordHash") String passwordHash, @Param("sessionVersion") int sessionVersion);

    @Insert("INSERT INTO sys_user_role (id, user_id, role_id) SELECT #{id}, #{userId}, id FROM sys_role WHERE code='EMPLOYEE_SELF_SERVICE' AND deleted=0")
    int assignEmployeeSelfServiceRole(@Param("id") long id, @Param("userId") long userId);

    @Update("UPDATE sys_user SET last_login_time = CURRENT_TIMESTAMP(3) WHERE id = #{id} AND deleted = 0")
    int updateLastLoginTime(@Param("id") long id);

    @Update("""
            UPDATE sys_user
            SET session_version = session_version + 1
            WHERE id = #{id}
              AND session_version = #{sessionVersion}
              AND status = 'ACTIVE'
              AND deleted = 0
            """)
    int incrementSessionVersion(@Param("id") long id, @Param("sessionVersion") int sessionVersion);
}
