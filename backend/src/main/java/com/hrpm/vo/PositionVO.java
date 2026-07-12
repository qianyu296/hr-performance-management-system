package com.hrpm.vo;

import com.hrpm.entity.Position;

public record PositionVO(String id, String code, String name, String jobFamily, String description,
                         int sortNo, String status, String version) {
    public static PositionVO from(Position value) {
        return new PositionVO(Long.toString(value.id()), value.code(), value.name(), value.jobFamily(),
                value.description(), value.sortNo(), value.status(), Integer.toString(value.version()));
    }
}
