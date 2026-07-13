package com.hrpm.vo;

import com.hrpm.entity.LeaveBalance;
import java.math.BigDecimal;

public record LeaveBalanceVO(String id, String employeeId, String balanceType, int balanceYear,
                             BigDecimal availableHours, BigDecimal frozenHours, String version) {
    public static LeaveBalanceVO from(LeaveBalance balance) {
        return new LeaveBalanceVO(Long.toString(balance.id()), Long.toString(balance.employeeId()), balance.balanceType(),
                balance.balanceYear(), balance.availableHours(), balance.frozenHours(), Integer.toString(balance.version()));
    }
}
