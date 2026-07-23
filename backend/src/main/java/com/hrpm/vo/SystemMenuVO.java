package com.hrpm.vo;

import com.hrpm.entity.SystemMenu;

public record SystemMenuVO(
        String id,
        String parentId,
        String name,
        String permissionCode,
        String menuType,
        String routePath,
        String status) {
    public static SystemMenuVO from(SystemMenu menu) {
        return new SystemMenuVO(
                Long.toString(menu.id()),
                menu.parentId() == null ? null : Long.toString(menu.parentId()),
                menu.name(),
                menu.permissionCode(),
                menu.menuType(),
                menu.routePath(),
                menu.status());
    }
}