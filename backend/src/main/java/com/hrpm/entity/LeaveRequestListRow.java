package com.hrpm.entity;




import com.hrpm.vo.LeaveRequestListVO;
import java.math.BigDecimal;
import java.time.Instant;

public record LeaveRequestListRow(
        long id,
        String requestNo,
        String leaveTypeName,
        Instant startTime,
        Instant endTime,
        BigDecimal durationHours,
        String status,
        Long workflowInstanceId,
        int version) {
    public LeaveRequestListVO toItem() {
        return new LeaveRequestListVO(Long.toString(id), requestNo, leaveTypeName, startTime, endTime, durationHours, status,
                workflowInstanceId == null ? null : Long.toString(workflowInstanceId), version);
    }
}
