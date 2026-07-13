package com.hrpm.security;

public record AuthenticatedUser(long userId, String username, int sessionVersion) {
}
