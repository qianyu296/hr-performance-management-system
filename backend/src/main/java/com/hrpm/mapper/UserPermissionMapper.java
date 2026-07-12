package com.hrpm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserPermissionMapper {
    @Select("""
            SELECT DISTINCT menu.permission_code
            FROM sys_user_role user_role
            JOIN sys_role role ON role.id = user_role.role_id AND role.deleted = 0 AND role.status = 'ACTIVE'
            JOIN sys_role_menu role_menu ON role_menu.role_id = role.id AND role_menu.deleted = 0
            JOIN sys_menu menu ON menu.id = role_menu.menu_id AND menu.deleted = 0 AND menu.status = 'ACTIVE'
            WHERE user_role.user_id = #{userId}
              AND user_role.deleted = 0
              AND menu.permission_code IS NOT NULL
            """)
    List<String> findPermissionCodesByUserId(@Param("userId") long userId);
}
