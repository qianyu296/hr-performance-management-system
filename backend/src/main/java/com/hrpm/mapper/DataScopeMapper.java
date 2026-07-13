package com.hrpm.mapper;

import com.hrpm.entity.RoleDataScope;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DataScopeMapper {
    @Select("""
            SELECT DISTINCT role_scope.scope_type AS scopeType, role_scope.scope_id AS scopeId
            FROM sys_user_role user_role
            JOIN sys_role role ON role.id = user_role.role_id AND role.status = 'ACTIVE' AND role.deleted = 0
            JOIN sys_role_data_scope role_scope ON role_scope.role_id = role.id AND role_scope.deleted = 0
            WHERE user_role.user_id = #{userId} AND user_role.deleted = 0
            """)
    List<RoleDataScope> findScopesByUserId(@Param("userId") long userId);

    @Select("""
            SELECT DISTINCT scope_dept.department_id
            FROM sys_user_role user_role
            JOIN sys_role role ON role.id = user_role.role_id AND role.status = 'ACTIVE' AND role.deleted = 0
            JOIN sys_role_data_scope role_scope ON role_scope.role_id = role.id AND role_scope.deleted = 0
            JOIN sys_data_scope_dept scope_dept ON scope_dept.scope_id = role_scope.scope_id AND scope_dept.deleted = 0
            WHERE user_role.user_id = #{userId}
              AND user_role.deleted = 0
              AND role_scope.scope_type = 'CUSTOM'
            """)
    List<Long> findCustomDepartmentIdsByUserId(@Param("userId") long userId);
}
