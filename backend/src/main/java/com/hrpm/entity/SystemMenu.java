package com.hrpm.entity;

public record SystemMenu(long id, Long parentId, String name, String permissionCode, String menuType,
                         String routePath, String status, int sortNo) {
}