package com.hrpm.common.exception;

public class DataScopeDeniedException extends RuntimeException {
    public DataScopeDeniedException() {
        super("当前账号无权访问该数据");
    }
}
