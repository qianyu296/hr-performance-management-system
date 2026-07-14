package com.hrpm.common.exception;


public class DepartmentNotFoundException extends ResourceNotFoundException {
    public DepartmentNotFoundException() {
        super("Parent Department does not exist");
    }
}
