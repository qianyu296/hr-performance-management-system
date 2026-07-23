package com.hrpm.vo;

import com.hrpm.entity.SystemRole;
import java.util.List;

public record SystemRoleDetailVO(
        String id,
        String code,
        String name,
        String status,
        String version,
        String dataScopeType,
        List<String> menuIds,
        List<String> departmentIds) {
    public static SystemRoleDetailVO from(SystemRole role, String dataScopeType, List<String> menuIds, List<String> departmentIds) {
        return new SystemRoleDetailVO(
                Long.toString(role.id()),
                role.code(),
                role.name(),
                role.status(),
                Integer.toString(role.version()),
                dataScopeType,
                menuIds,
                departmentIds);
    }
}