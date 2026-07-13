package com.hrpm.vo;

import com.hrpm.entity.LeaveBalanceChange;
import java.math.BigDecimal;
import java.time.Instant;

public record LeaveBalanceChangeVO(String id, String balanceType, BigDecimal deltaHours, BigDecimal beforeHours,
                                   BigDecimal afterHours, String sourceType, String reason, String createdBy,
                                   Instant createdTime) {
    public static LeaveBalanceChangeVO from(LeaveBalanceChange change) {
        return new LeaveBalanceChangeVO(Long.toString(change.id()), change.balanceType(), change.deltaHours(),
                change.beforeHours(), change.afterHours(), change.sourceType(), change.reason(),
                change.createdBy() == null ? null : Long.toString(change.createdBy()), change.createdTime());
    }
}
