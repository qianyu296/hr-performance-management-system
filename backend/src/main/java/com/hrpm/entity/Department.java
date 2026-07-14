package com.hrpm.entity;

import java.time.LocalDate;

public record Department(
        long id,
        String code,
        String name,
        Long parentId,
        Long leaderEmployeeId,
        String path,
        int sortNo,
        LocalDate effectiveDate,
        String status,
        int version) {
}
