package com.hrpm.vo;

import com.hrpm.entity.OvertimeRequestRecord;
import java.math.BigDecimal;

public record OvertimeRequestVO(String id, String requestNo, String status, BigDecimal durationHours,
                                 String compensationType) {
    public static OvertimeRequestVO from(OvertimeRequestRecord request) {
        return new OvertimeRequestVO(Long.toString(request.id()), request.requestNo(), request.status(),
                request.durationHours(), request.compensationType());
    }
}
