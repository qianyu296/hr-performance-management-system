package com.hrpm.mapper;

import com.hrpm.entity.UserAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
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

    @Select("""
            SELECT id, username, password_hash AS passwordHash, employee_id AS employeeId, status, session_version AS sessionVersion,
                   password_change_required AS passwordChangeRequired
            FROM sys_user
            WHERE employee_id = #{employeeId} AND deleted = 0
            """)
    UserAccount findByEmployeeId(@Param("employeeId") long employeeId);

    @Insert("INSERT INTO sys_user (id, username, password_hash, employee_id, status, session_version, password_change_required) VALUES (#{id}, #{username}, #{passwordHash}, #{employeeId}, 'ACTIVE', 0, 1)")
    int insertEmployeeAccount(@Param("id") long id, @Param("username") String username, @Param("passwordHash") String passwordHash, @Param("employeeId") long employeeId);

    @Update("UPDATE sys_user SET password_hash=#{passwordHash}, password_change_required=0, session_version=session_version+1 WHERE id=#{id} AND session_version=#{sessionVersion} AND deleted=0")
    int changePassword(@Param("id") long id, @Param("passwordHash") String passwordHash, @Param("sessionVersion") int sessionVersion);

    @Insert("INSERT INTO sys_user_role (id, user_id, role_id) SELECT #{id}, #{userId}, id FROM sys_role WHERE code='EMPLOYEE_SELF_SERVICE' AND deleted=0")
    int assignEmployeeSelfServiceRole(@Param("id") long id, @Param("userId") long userId);

    @Insert("INSERT INTO sys_user_role (id, user_id, role_id) SELECT #{id}, #{userId}, id FROM sys_role WHERE code=#{roleCode} AND deleted=0")
    int assignRoleByCode(@Param("id") long id, @Param("userId") long userId, @Param("roleCode") String roleCode);

    @Select("""
            SELECT COUNT(*)
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
            WHERE ur.user_id = #{userId}
              AND r.code = #{roleCode}
              AND ur.deleted = 0
            """)
    int countRoleByCode(@Param("userId") long userId, @Param("roleCode") String roleCode);

    @Update("""
            UPDATE sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
            SET ur.deleted = 1,
                ur.version = ur.version + 1
            WHERE ur.user_id = #{userId}
              AND r.code = #{roleCode}
              AND ur.deleted = 0
            """)
    int deleteRoleByCode(@Param("userId") long userId, @Param("roleCode") String roleCode);

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

    @Update("""
            UPDATE sys_user
            SET status = 'DISABLED',
                session_version = session_version + 1
            WHERE employee_id = #{employeeId}
              AND deleted = 0
              AND status = 'ACTIVE'
            """)
    int disableForEmployee(@Param("employeeId") long employeeId);

    @Update("""
            UPDATE sys_user
            SET status = 'ACTIVE',
                session_version = session_version + 1
            WHERE employee_id = #{employeeId}
              AND deleted = 0
              AND status <> 'ACTIVE'
            """)
    int activateByEmployeeId(@Param("employeeId") long employeeId);
}