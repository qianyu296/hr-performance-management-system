package com.hrpm.vo;

import com.hrpm.entity.AuthorizedMenu;

public record AuthorizedMenuVO(String id, String name, String menuType, String routePath, String permissionCode) {
    public static AuthorizedMenuVO from(AuthorizedMenu menu) {
        return new AuthorizedMenuVO(
                Long.toString(menu.id()), menu.name(), menu.menuType(), menu.routePath(), menu.permissionCode());
    }
}
