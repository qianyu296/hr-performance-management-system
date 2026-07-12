package com.hrpm.entity;

public record LeaveType(long id, String code, String name, boolean deductBalance, String status) {
}
