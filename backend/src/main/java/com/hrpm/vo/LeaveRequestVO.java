package com.hrpm.vo;


import com.hrpm.entity.LeaveRequestRecord;

import java.math.BigDecimal;

public record LeaveRequestVO(String id, String requestNo, String status, BigDecimal durationHours) {
    public static LeaveRequestVO from(LeaveRequestRecord request) {
        return new LeaveRequestVO(
                Long.toString(request.id()), request.requestNo(), request.status(), request.durationHours());
    }
}
