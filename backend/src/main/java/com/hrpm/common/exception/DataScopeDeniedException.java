package com.hrpm.common.exception;

public class DataScopeDeniedException extends RuntimeException {
    public DataScopeDeniedException() {
        super("Data scope denied");
    }
}
