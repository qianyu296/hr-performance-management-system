package com.hrpm.entity;

import java.time.LocalDate;

public record Department(
        long id,
        String code,
        String name,
        Long parentId,
        String path,
        LocalDate effectiveDate,
        String status) {
}
