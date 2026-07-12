package com.hrpm.vo;


import com.hrpm.entity.LeaveType;

public record LeaveTypeVO(String id, String code, String name, boolean deductBalance) {
    public static LeaveTypeVO from(LeaveType leaveType) {
        return new LeaveTypeVO(Long.toString(leaveType.id()), leaveType.code(), leaveType.name(), leaveType.deductBalance());
    }
}
