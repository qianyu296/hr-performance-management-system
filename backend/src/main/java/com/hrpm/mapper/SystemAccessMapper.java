package com.hrpm.mapper;

import com.hrpm.entity.Department;
import com.hrpm.entity.RoleDataScope;
import com.hrpm.entity.SystemMenu;
import com.hrpm.entity.SystemRole;
import com.hrpm.entity.SystemUser;
import java.util.List;
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
            WHERE id = #{id} AND deleted = 0
            """)
    SystemRole findRoleById(@Param("id") long id);

    @Select("""
            SELECT id, code, name, status, version
            FROM sys_role
            WHERE code = #{code} AND deleted = 0
            """)
    SystemRole findRoleByCode(@Param("code") String code);

    @Select("""
            SELECT id, code, name, status, version
            FROM sys_role
            WHERE id = #{id} AND deleted = 0 AND status = 'ACTIVE'
            """)
    SystemRole findActiveRoleById(@Param("id") long id);

    @Select("""
            SELECT id, parent_id AS parentId, name, permission_code AS permissionCode, menu_type AS menuType,
                   route_path AS routePath, status, sort_no AS sortNo
            FROM sys_menu
            WHERE deleted = 0
            ORDER BY sort_no, id
            """)
    List<SystemMenu> findMenus();

    @Select("SELECT COUNT(*) FROM sys_menu WHERE id = #{id} AND deleted = 0 AND status = 'ACTIVE'")
    int countActiveMenuById(@Param("id") long id);

    @Select("""
            SELECT rm.menu_id
            FROM sys_role_menu rm
            JOIN sys_menu m ON m.id = rm.menu_id
            WHERE rm.role_id = #{roleId}
              AND rm.deleted = 0
              AND m.deleted = 0
              AND m.status = 'ACTIVE'
            ORDER BY rm.menu_id
            """)
    List<Long> findMenuIdsByRoleId(@Param("roleId") long roleId);

    @Select("""
            SELECT scope_type AS scopeType, scope_id AS scopeId
            FROM sys_role_data_scope
            WHERE role_id = #{roleId} AND deleted = 0
            ORDER BY id
            """)
    List<RoleDataScope> findRoleScopesByRoleId(@Param("roleId") long roleId);

    @Select("SELECT scope_id FROM sys_role_data_scope WHERE role_id = #{roleId} AND deleted = 0 AND scope_id IS NOT NULL ORDER BY scope_id")
    List<Long> findScopeIdsByRoleId(@Param("roleId") long roleId);

    @Select("SELECT department_id FROM sys_data_scope_dept WHERE scope_id = #{scopeId} AND deleted = 0 ORDER BY department_id")
    List<Long> findDepartmentIdsByScopeId(@Param("scopeId") long scopeId);

    @Select("""
            SELECT id, code, name, parent_id AS parentId, leader_employee_id AS leaderEmployeeId,
                   path, sort_no AS sortNo, effective_date AS effectiveDate, status, version
            FROM hr_department
            WHERE deleted = 0
            ORDER BY path, sort_no, id
            """)
    List<Department> findAllDepartments();

    @Select("SELECT COUNT(*) FROM hr_department WHERE id = #{id} AND deleted = 0 AND status = 'ACTIVE'")
    int countActiveDepartmentById(@Param("id") long id);

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

    @org.apache.ibatis.annotations.Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteRolesForUser(@Param("userId") long userId);

    @Insert("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (#{id}, #{userId}, #{roleId})")
    int insertUserRole(@Param("id") long id, @Param("userId") long userId, @Param("roleId") long roleId);

    @Insert("INSERT INTO sys_role (id, code, name, status) VALUES (#{id}, #{code}, #{name}, #{status})")
    int insertRole(@Param("id") long id, @Param("code") String code, @Param("name") String name, @Param("status") String status);

    @Update("""
            UPDATE sys_role
            SET name = #{name},
                status = #{status},
                version = version + 1
            WHERE id = #{id}
              AND version = #{version}
              AND deleted = 0
            """)
    int updateRole(@Param("id") long id, @Param("name") String name, @Param("status") String status, @Param("version") int version);

    @org.apache.ibatis.annotations.Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteRoleMenus(@Param("roleId") long roleId);

    @Insert("INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES (#{id}, #{roleId}, #{menuId})")
    int insertRoleMenu(@Param("id") long id, @Param("roleId") long roleId, @Param("menuId") long menuId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM sys_data_scope_dept WHERE scope_id = #{scopeId}")
    int deleteScopeDepartments(@Param("scopeId") long scopeId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM sys_role_data_scope WHERE role_id = #{roleId}")
    int deleteRoleScopes(@Param("roleId") long roleId);

    @Insert("INSERT INTO sys_role_data_scope (id, role_id, scope_type, scope_id) VALUES (#{id}, #{roleId}, #{scopeType}, #{scopeId})")
    int insertRoleScope(@Param("id") long id, @Param("roleId") long roleId, @Param("scopeType") String scopeType, @Param("scopeId") Long scopeId);

    @Insert("INSERT INTO sys_data_scope_dept (id, scope_id, department_id) VALUES (#{id}, #{scopeId}, #{departmentId})")
    int insertScopeDepartment(@Param("id") long id, @Param("scopeId") long scopeId, @Param("departmentId") long departmentId);

    @Update("""
            UPDATE sys_user
            SET session_version = session_version + 1, version = version + 1
            WHERE id = #{id} AND version = #{version} AND deleted = 0
            """)
    int updateRolesVersionAndRevokeSessions(@Param("id") long id, @Param("version") int version);
}