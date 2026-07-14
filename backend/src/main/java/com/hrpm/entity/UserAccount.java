package com.hrpm.entity;

public record UserAccount(long id, String username, String passwordHash, Long employeeId, String status, int sessionVersion,
                          boolean passwordChangeRequired) {
    public UserAccount(long id, String username, String passwordHash, Long employeeId, String status, int sessionVersion) {
        this(id, username, passwordHash, employeeId, status, sessionVersion, false);
    }
}
