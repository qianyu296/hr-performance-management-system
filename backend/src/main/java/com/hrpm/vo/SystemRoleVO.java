package com.hrpm.vo;

import com.hrpm.entity.SystemRole;

public record SystemRoleVO(String id, String code, String name, String status, String version) {
    public static SystemRoleVO from(SystemRole role) {
        return new SystemRoleVO(Long.toString(role.id()), role.code(), role.name(), role.status(), Integer.toString(role.version()));
    }
}
