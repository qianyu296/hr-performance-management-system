package com.hrpm.common.exception;

public class VersionConflictException extends RuntimeException {
    public VersionConflictException() {
        super("The resource has changed. Refresh and try again.");
    }
}
