package com.hrpm.common.exception;

public class VersionConflictException extends RuntimeException {
    public VersionConflictException() {
        super("资源已发生变更，请刷新后重试");
    }
}
