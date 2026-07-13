package com.hrpm.entity;

import com.hrpm.vo.OvertimeRequestListVO;
import java.math.BigDecimal;
import java.time.Instant;

public record OvertimeRequestListRow(long id, String requestNo, Instant startTime, Instant endTime,
                                     BigDecimal durationHours, String compensationType, String status,
                                     Long workflowInstanceId, int version) {
    public OvertimeRequestListVO toItem() {
        return new OvertimeRequestListVO(Long.toString(id), requestNo, startTime, endTime, durationHours,
                compensationType, status, workflowInstanceId == null ? null : Long.toString(workflowInstanceId), version);
    }
}
