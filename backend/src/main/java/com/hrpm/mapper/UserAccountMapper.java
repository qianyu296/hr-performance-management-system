package com.hrpm.mapper;


import com.hrpm.entity.UserAccount;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserAccountMapper {
    @Select("""
            SELECT id, username, password_hash AS passwordHash, employee_id AS employeeId, status, session_version AS sessionVersion
            FROM sys_user
            WHERE username = #{username} AND deleted = 0
            """)
    UserAccount findByUsername(@Param("username") String username);

    @Select("""
            SELECT id, username, password_hash AS passwordHash, employee_id AS employeeId, status, session_version AS sessionVersion
            FROM sys_user
            WHERE id = #{id} AND deleted = 0
            """)
    UserAccount findById(@Param("id") long id);

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
