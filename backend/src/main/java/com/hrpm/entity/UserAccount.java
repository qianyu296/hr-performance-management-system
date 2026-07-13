package com.hrpm.entity;

public record UserAccount(long id, String username, String passwordHash, Long employeeId, String status, int sessionVersion) {
}
