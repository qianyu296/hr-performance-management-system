package com.hrpm.service;


import com.hrpm.common.exception.IllegalLeaveStateTransitionException;
import com.hrpm.entity.BalanceChange;
import com.hrpm.entity.BalanceChangeType;
import com.hrpm.entity.LeaveApprovalResult;
import com.hrpm.entity.LeaveBalance;
import com.hrpm.entity.LeaveCancellationResult;
import com.hrpm.entity.LeaveRequest;
import com.hrpm.entity.LeaveRequestStatus;

import java.math.BigDecimal;

public class LeaveApprovalService {
    public LeaveApprovalResult approve(LeaveRequest request, LeaveBalance balance) {
        requireSameBalanceOwner(request, balance);
        if (request.status() != LeaveRequestStatus.IN_PROGRESS) {
            throw new IllegalLeaveStateTransitionException();
        }
        BigDecimal delta = request.durationHours().negate();
        LeaveBalance updatedBalance = balance.changeBy(delta);
        return new LeaveApprovalResult(
                request.withStatus(LeaveRequestStatus.APPROVED),
                updatedBalance,
                new BalanceChange(balance.id(), request.id(), BalanceChangeType.LEAVE_APPROVAL, delta,
                        balance.availableHours(), updatedBalance.availableHours()));
    }

    public LeaveCancellationResult cancel(LeaveRequest request, LeaveBalance balance) {
        requireSameBalanceOwner(request, balance);
        if (request.status() != LeaveRequestStatus.APPROVED) {
            throw new IllegalLeaveStateTransitionException();
        }
        BigDecimal delta = request.durationHours();
        LeaveBalance updatedBalance = balance.changeBy(delta);
        return new LeaveCancellationResult(
                request.withStatus(LeaveRequestStatus.CANCELLED),
                updatedBalance,
                new BalanceChange(balance.id(), request.id(), BalanceChangeType.LEAVE_CANCELLATION, delta,
                        balance.availableHours(), updatedBalance.availableHours()));
    }

    private void requireSameBalanceOwner(LeaveRequest request, LeaveBalance balance) {
        if (request.employeeId() != balance.employeeId() || !request.balanceType().equals(balance.balanceType())) {
            throw new IllegalArgumentException("Leave request and balance do not match");
        }
    }
}
