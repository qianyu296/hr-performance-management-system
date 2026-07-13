package com.hrpm.common.exception;

public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException() {
        super("Username or password is invalid");
    }
}
