package com.hrpm.entity;

public record AuthorizedMenu(long id, String name, String menuType, String routePath, String permissionCode, int sortNo) {
}
