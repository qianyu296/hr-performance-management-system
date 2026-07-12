package com.hrpm.entity;

public record Position(long id, String code, String name, String jobFamily, String description,
                       int sortNo, String status, int version) {
}
