package com.hrpm.common.exception;

public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException() {
        super("用户名或密码错误");
    }
}
