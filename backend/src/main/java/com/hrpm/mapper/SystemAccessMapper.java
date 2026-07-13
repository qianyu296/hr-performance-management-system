package com.hrpm.mapper;

import com.hrpm.entity.SystemRole;
import com.hrpm.entity.SystemUser;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SystemAccessMapper {
    @Select("""
            SELECT id, code, name, status, version
            FROM sys_role
            WHERE deleted = 0
            ORDER BY code
            """)
    List<SystemRole> findRoles();

    @Select("""
            SELECT id, code, name, status, version
            FROM sys_role
            WHERE id = #{id} AND deleted = 0 AND status = 'ACTIVE'
            """)
    SystemRole findActiveRoleById(@Param("id") long id);

    @Select("""
            SELECT id, username, employee_id AS employeeId, status, session_version AS sessionVersion, version
            FROM sys_user
            WHERE deleted = 0
            ORDER BY username
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<SystemUser> findUsers(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted = 0")
    long countUsers();

    @Select("""
            SELECT id, username, employee_id AS employeeId, status, session_version AS sessionVersion, version
            FROM sys_user
            WHERE id = #{id} AND deleted = 0
            """)
    SystemUser findUserById(@Param("id") long id);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId} AND deleted = 0 ORDER BY role_id")
    List<Long> findRoleIdsByUserId(@Param("userId") long userId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteRolesForUser(@Param("userId") long userId);

    @Insert("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (#{id}, #{userId}, #{roleId})")
    int insertUserRole(@Param("id") long id, @Param("userId") long userId, @Param("roleId") long roleId);

    @Update("""
            UPDATE sys_user
            SET session_version = session_version + 1, version = version + 1
            WHERE id = #{id} AND version = #{version} AND deleted = 0
            """)
    int updateRolesVersionAndRevokeSessions(@Param("id") long id, @Param("version") int version);
}
