package com.hrpm.entity;

public record SystemUser(long id, String username, Long employeeId, String status, int sessionVersion, int version) {
}
