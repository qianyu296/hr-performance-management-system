package com.hrpm.common.exception;


import com.hrpm.entity.Department;

public class DepartmentNotFoundException extends RuntimeException {
    public DepartmentNotFoundException() {
        super("Parent Department does not exist");
    }
}
