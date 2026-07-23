package com.hrpm.entity;
import com.hrpm.vo.WorkflowTaskListVO;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
public record WorkflowTaskListRow(
        long id,
        long instanceId,
        String businessType,
        long businessId,
        String requestNo,
        String applicantName,
        String leaveTypeName,
        Instant startTime,
        Instant endTime,
        LocalDate effectiveDate,
        BigDecimal durationHours,
        String status,
        int version) {
    public WorkflowTaskListVO toItem() {
        return new WorkflowTaskListVO(Long.toString(id), Long.toString(instanceId), businessType, Long.toString(businessId), requestNo, applicantName,
                leaveTypeName, startTime, endTime, effectiveDate, durationHours, status, version);
    }
}