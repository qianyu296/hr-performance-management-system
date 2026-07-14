package com.hrpm.vo;


import com.hrpm.entity.LeaveType;

public record LeaveTypeVO(String id, String code, String name, boolean deductBalance, java.math.BigDecimal minUnitHours,
                          java.math.BigDecimal annualQuota, String status, String version) {
    public static LeaveTypeVO from(LeaveType leaveType) {
        return new LeaveTypeVO(Long.toString(leaveType.id()), leaveType.code(), leaveType.name(), leaveType.deductBalance(),
                leaveType.minUnitHours(), leaveType.annualQuota(), leaveType.status(), Integer.toString(leaveType.version()));
    }
}
