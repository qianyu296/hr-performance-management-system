package com.hrpm.entity;

public record LeaveType(long id, String code, String name, boolean deductBalance, java.math.BigDecimal minUnitHours,
                        java.math.BigDecimal annualQuota, String status, int version) {
}
