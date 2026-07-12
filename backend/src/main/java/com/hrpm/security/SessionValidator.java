package com.hrpm.security;

@FunctionalInterface
public interface SessionValidator {
    boolean isValid(AuthenticatedUser user);
}
