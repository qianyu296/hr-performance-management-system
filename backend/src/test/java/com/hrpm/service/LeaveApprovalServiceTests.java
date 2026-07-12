package com.hrpm.service;


import com.hrpm.common.exception.IllegalLeaveStateTransitionException;
import com.hrpm.common.exception.InsufficientLeaveBalanceException;
import com.hrpm.entity.BalanceChange;
import com.hrpm.entity.BalanceChangeType;
import com.hrpm.entity.LeaveApprovalResult;
import com.hrpm.entity.LeaveBalance;
import com.hrpm.entity.LeaveCancellationResult;
import com.hrpm.entity.LeaveRequest;
import com.hrpm.entity.LeaveRequestStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LeaveApprovalServiceTests {
    private final LeaveApprovalService service = new LeaveApprovalService();

    @Test
    void approvalDebitsBalanceAndCreatesImmutableDebitChange() {
        LeaveRequest request = LeaveRequest.inProgress(100L, 9L, "ANNUAL", new BigDecimal("8.00"));
        LeaveBalance balance = new LeaveBalance(12L, 9L, "ANNUAL", new BigDecimal("16.00"), 4);

        LeaveApprovalResult result = service.approve(request, balance);

        assertEquals(LeaveRequestStatus.APPROVED, result.request().status());
        assertEquals(new BigDecimal("8.00"), result.balance().availableHours());
        assertEquals(new BigDecimal("-8.00"), result.BalanceChange().deltaHours());
        assertEquals(BalanceChangeType.LEAVE_APPROVAL, result.BalanceChange().type());
        assertEquals(5, result.balance().version());
    }

    @Test
    void approvalRejectsRequestWhenAvailableBalanceIsInsufficient() {
        LeaveRequest request = LeaveRequest.inProgress(100L, 9L, "ANNUAL", new BigDecimal("8.00"));
        LeaveBalance balance = new LeaveBalance(12L, 9L, "ANNUAL", new BigDecimal("7.50"), 4);

        assertThrows(InsufficientLeaveBalanceException.class, () -> service.approve(request, balance));
    }

    @Test
    void cancellationRestoresBalanceExactlyOnceThroughReversalChange() {
        LeaveRequest approvedRequest = LeaveRequest.approved(100L, 9L, "ANNUAL", new BigDecimal("8.00"));
        LeaveBalance balance = new LeaveBalance(12L, 9L, "ANNUAL", new BigDecimal("8.00"), 5);

        LeaveCancellationResult result = service.cancel(approvedRequest, balance);

        assertEquals(LeaveRequestStatus.CANCELLED, result.request().status());
        assertEquals(new BigDecimal("16.00"), result.balance().availableHours());
        assertEquals(new BigDecimal("8.00"), result.BalanceChange().deltaHours());
        assertEquals(BalanceChangeType.LEAVE_CANCELLATION, result.BalanceChange().type());
        assertThrows(IllegalLeaveStateTransitionException.class, () -> service.cancel(result.request(), result.balance()));
    }
}
