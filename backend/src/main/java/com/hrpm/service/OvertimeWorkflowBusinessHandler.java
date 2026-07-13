package com.hrpm.service;

import com.hrpm.common.IdGenerator;
import com.hrpm.entity.LeaveBalanceRow;
import com.hrpm.entity.OvertimeRequestSubmission;
import com.hrpm.entity.WorkflowBusinessContext;
import com.hrpm.mapper.OvertimeRequestMapper;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;

@Service
public class OvertimeWorkflowBusinessHandler implements WorkflowBusinessHandler {
    private final OvertimeRequestMapper overtimeRequestMapper;
    private final IdGenerator idGenerator;

    public OvertimeWorkflowBusinessHandler(OvertimeRequestMapper overtimeRequestMapper, IdGenerator idGenerator) {
        this.overtimeRequestMapper = overtimeRequestMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public String businessType() { return "OVERTIME"; }

    @Override
    public void approve(WorkflowBusinessContext context) {
        OvertimeRequestSubmission request = requireInProgress(context.businessId());
        if (!"TIME_OFF".equals(request.compensationType())) {
            if (overtimeRequestMapper.approveRequest(request.id(), request.version()) != 1) {
                throw new IllegalStateException("Overtime request changed before approval");
            }
            return;
        }
        int year = request.startTime().atZone(ZoneOffset.UTC).getYear();
        LeaveBalanceRow balance = overtimeRequestMapper.findTimeOffBalance(request.employeeId(), year);
        if (balance == null) {
            overtimeRequestMapper.insertTimeOffBalance(idGenerator.nextId(), request.employeeId(), year, context.actorUserId());
            balance = overtimeRequestMapper.findTimeOffBalance(request.employeeId(), year);
        }
        BigDecimal after = balance.availableHours().add(request.durationHours());
        if (overtimeRequestMapper.approveRequest(request.id(), request.version()) != 1
                || overtimeRequestMapper.updateBalance(balance.id(), balance.version(), after) != 1) {
            throw new IllegalStateException("Overtime request changed before approval");
        }
        overtimeRequestMapper.insertBalanceChange(idGenerator.nextId(), balance.id(), request.employeeId(), request.durationHours(),
                balance.availableHours(), after, "TIME_OFF", request.id(), "Overtime approved as time off", context.actorUserId());
    }

    @Override
    public void reject(WorkflowBusinessContext context) {
        OvertimeRequestSubmission request = requireInProgress(context.businessId());
        if (overtimeRequestMapper.rejectRequest(request.id(), request.version()) != 1) throw new IllegalStateException("Overtime request changed before rejection");
    }

    @Override
    public void withdraw(WorkflowBusinessContext context) { returnToDraft(context); }

    @Override
    public void returnToDraft(WorkflowBusinessContext context) {
        OvertimeRequestSubmission request = requireInProgress(context.businessId());
        if (overtimeRequestMapper.returnRequestToDraft(request.id(), request.version()) != 1) throw new IllegalStateException("Overtime request changed before return");
    }

    private OvertimeRequestSubmission requireInProgress(long requestId) {
        OvertimeRequestSubmission request = overtimeRequestMapper.findSubmission(requestId);
        if (request == null || !"IN_PROGRESS".equals(request.status())) throw new IllegalStateException("Overtime request is not in progress");
        return request;
    }
}
