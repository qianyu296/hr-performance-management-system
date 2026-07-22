package com.hrpm.common.exception;

public class DepartmentNotFoundException extends ResourceNotFoundException {
    public DepartmentNotFoundException() {
        super("上级部门不存在");
    }
}
