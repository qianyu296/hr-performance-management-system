package com.hrpm.vo;

import com.hrpm.entity.SystemUser;
import java.util.List;

public record SystemUserVO(String id, String username, String employeeId, String status, List<String> roleIds, String version) {
    public static SystemUserVO from(SystemUser user, List<String> roleIds) {
        return new SystemUserVO(
                Long.toString(user.id()),
                user.username(),
                user.employeeId() == null ? null : Long.toString(user.employeeId()),
                user.status(),
                roleIds,
                Integer.toString(user.version()));
    }
}
