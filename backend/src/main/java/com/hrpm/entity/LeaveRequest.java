package com.hrpm.entity;

import java.math.BigDecimal;

public record LeaveRequest(long id, long employeeId, String balanceType, BigDecimal durationHours, LeaveRequestStatus status) {
    public LeaveRequest {
        if (id <= 0 || employeeId <= 0 || balanceType == null || balanceType.isBlank()
                || durationHours == null || durationHours.signum() <= 0 || status == null) {
            throw new IllegalArgumentException("Invalid leave request");
        }
    }

    public static LeaveRequest inProgress(long id, long employeeId, String balanceType, BigDecimal durationHours) {
        return new LeaveRequest(id, employeeId, balanceType, durationHours, LeaveRequestStatus.IN_PROGRESS);
    }

    public static LeaveRequest approved(long id, long employeeId, String balanceType, BigDecimal durationHours) {
        return new LeaveRequest(id, employeeId, balanceType, durationHours, LeaveRequestStatus.APPROVED);
    }

    public LeaveRequest withStatus(LeaveRequestStatus nextStatus) {
        return new LeaveRequest(id, employeeId, balanceType, durationHours, nextStatus);
    }
}
