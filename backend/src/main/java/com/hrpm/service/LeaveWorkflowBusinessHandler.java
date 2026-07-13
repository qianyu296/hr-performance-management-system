package com.hrpm.service;

import com.hrpm.common.IdGenerator;
import com.hrpm.entity.LeaveBalanceRow;
import com.hrpm.entity.LeaveRequestSubmission;
import com.hrpm.entity.WorkflowBusinessContext;
import com.hrpm.mapper.LeaveRequestMapper;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;

@Service
public class LeaveWorkflowBusinessHandler implements WorkflowBusinessHandler {
    private final LeaveRequestMapper leaveRequestMapper;
    private final IdGenerator idGenerator;

    public LeaveWorkflowBusinessHandler(LeaveRequestMapper leaveRequestMapper, IdGenerator idGenerator) {
        this.leaveRequestMapper = leaveRequestMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public String businessType() {
        return "LEAVE";
    }

    @Override
    public void approve(WorkflowBusinessContext context) {
        LeaveRequestSubmission request = requireInProgress(context.businessId());
        if (!request.deductBalance()) {
            if (leaveRequestMapper.approveRequest(request.id(), request.version()) != 1) {
                throw new IllegalStateException("Leave request changed before approval");
            }
            return;
        }
        LeaveBalanceRow balance = leaveRequestMapper.findBalance(request.employeeId(), request.balanceType(),
                request.startTime().atZone(ZoneOffset.UTC).getYear());
        if (balance == null || balance.availableHours().compareTo(request.durationHours()) < 0) {
            throw new IllegalStateException("Leave balance is insufficient");
        }
        BigDecimal after = balance.availableHours().subtract(request.durationHours());
        if (leaveRequestMapper.approveRequest(request.id(), request.version()) != 1
                || leaveRequestMapper.updateBalance(balance.id(), balance.version(), after) != 1) {
            throw new IllegalStateException("Leave request changed before approval");
        }
        leaveRequestMapper.insertBalanceChange(idGenerator.nextId(), balance.id(), request.employeeId(), request.balanceType(),
                request.durationHours().negate(), balance.availableHours(), after, request.id());
    }

    @Override
    public void reject(WorkflowBusinessContext context) {
        LeaveRequestSubmission request = requireInProgress(context.businessId());
        if (leaveRequestMapper.rejectRequest(request.id(), request.version()) != 1) {
            throw new IllegalStateException("Leave request changed before rejection");
        }
    }

    @Override
    public void withdraw(WorkflowBusinessContext context) {
        returnToDraft(context);
    }

    @Override
    public void returnToDraft(WorkflowBusinessContext context) {
        LeaveRequestSubmission request = requireInProgress(context.businessId());
        if (leaveRequestMapper.returnRequestToDraft(request.id(), request.version()) != 1) {
            throw new IllegalStateException("Leave request changed before return");
        }
    }

    private LeaveRequestSubmission requireInProgress(long requestId) {
        LeaveRequestSubmission request = leaveRequestMapper.findSubmission(requestId);
        if (request == null || !"IN_PROGRESS".equals(request.status())) {
            throw new IllegalStateException("Leave request is not in progress");
        }
        return request;
    }
}
