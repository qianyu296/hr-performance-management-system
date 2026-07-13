package com.hrpm.service;

import com.hrpm.common.IdGenerator;
import com.hrpm.dto.CreateOvertimeRequestDTO;
import com.hrpm.entity.OvertimeRequestListRow;
import com.hrpm.entity.OvertimeRequestRecord;
import com.hrpm.entity.OvertimeRequestSubmission;
import com.hrpm.entity.UserAccount;
import com.hrpm.entity.LeaveBalanceRow;
import com.hrpm.mapper.EmployeeAttendanceMapper;
import com.hrpm.mapper.OvertimeRequestMapper;
import com.hrpm.mapper.UserAccountMapper;
import com.hrpm.vo.OvertimeRequestListVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OvertimeRequestService {
    private final UserAccountMapper userAccountMapper;
    private final EmployeeAttendanceMapper employeeAttendanceMapper;
    private final OvertimeRequestMapper overtimeRequestMapper;
    private final OvertimeWorkflowService overtimeWorkflowService;
    private final IdGenerator idGenerator;

    public OvertimeRequestService(UserAccountMapper userAccountMapper, EmployeeAttendanceMapper employeeAttendanceMapper,
                                  OvertimeRequestMapper overtimeRequestMapper, OvertimeWorkflowService overtimeWorkflowService,
                                  IdGenerator idGenerator) {
        this.userAccountMapper = userAccountMapper;
        this.employeeAttendanceMapper = employeeAttendanceMapper;
        this.overtimeRequestMapper = overtimeRequestMapper;
        this.overtimeWorkflowService = overtimeWorkflowService;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public OvertimeRequestRecord createDraft(long userId, CreateOvertimeRequestDTO command) {
        UserAccount account = employeeAccount(userId);
        if (!command.endTime().isAfter(command.startTime())) throw new IllegalArgumentException("Overtime end time must be after start time");
        if (!"TIME_OFF".equals(command.compensationType()) && !"OVERTIME_PAY".equals(command.compensationType())) {
            throw new IllegalArgumentException("Invalid overtime compensation type");
        }
        BigDecimal durationHours = BigDecimal.valueOf(Duration.between(command.startTime(), command.endTime()).toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        if (durationHours.signum() <= 0) throw new IllegalArgumentException("Overtime duration must be positive");
        long id = idGenerator.nextId();
        OvertimeRequestRecord request = new OvertimeRequestRecord(id, "OT" + id, account.employeeId(), command.startTime(),
                command.endTime(), durationHours, command.reason(), command.compensationType(), "DRAFT", "{}");
        overtimeRequestMapper.insert(request);
        return request;
    }

    public OvertimeRequestRecord submit(long userId, long requestId, int version) {
        OvertimeRequestSubmission request = ownedRequest(userId, requestId);
        if (request.version() != version) throw new IllegalStateException("Overtime request version is stale");
        overtimeWorkflowService.submit(userId, request);
        return new OvertimeRequestRecord(request.id(), "", request.employeeId(), request.startTime(), request.endTime(),
                request.durationHours(), "", request.compensationType(), "IN_PROGRESS", "{}");
    }

    public List<OvertimeRequestListVO> listForUser(long userId) {
        return overtimeRequestMapper.listByEmployeeId(employeeAccount(userId).employeeId()).stream()
                .map(OvertimeRequestListRow::toItem).toList();
    }

    @Transactional
    public OvertimeRequestRecord cancel(long userId, long requestId, int version) {
        OvertimeRequestSubmission request = ownedRequest(userId, requestId);
        if (!"APPROVED".equals(request.status()) || request.version() != version) {
            throw new IllegalStateException("Overtime request is not cancellable");
        }
        LeaveBalanceRow balance = null;
        BigDecimal after = null;
        if ("TIME_OFF".equals(request.compensationType())) {
            balance = overtimeRequestMapper.findTimeOffBalance(request.employeeId(), request.startTime().atZone(ZoneOffset.UTC).getYear());
            if (balance == null || balance.availableHours().compareTo(request.durationHours()) < 0) {
                throw new IllegalStateException("Time-off balance changed before cancellation");
            }
            after = balance.availableHours().subtract(request.durationHours());
        }
        if (overtimeRequestMapper.cancelRequest(request.id(), request.employeeId(), version) != 1) {
            throw new IllegalStateException("Overtime request changed before cancellation");
        }
        if (balance != null) {
            if (overtimeRequestMapper.updateBalance(balance.id(), balance.version(), after) != 1) {
                throw new IllegalStateException("Time-off balance changed before cancellation");
            }
            overtimeRequestMapper.insertBalanceChange(idGenerator.nextId(), balance.id(), request.employeeId(), request.durationHours().negate(),
                    balance.availableHours(), after, "OVERTIME_CANCELLATION", request.id(), "Overtime time-off cancelled", userId);
        }
        return new OvertimeRequestRecord(request.id(), "", request.employeeId(), request.startTime(), request.endTime(),
                request.durationHours(), "", request.compensationType(), "CANCELLED", "{}");
    }

    private OvertimeRequestSubmission ownedRequest(long userId, long requestId) {
        UserAccount account = employeeAccount(userId);
        OvertimeRequestSubmission request = overtimeRequestMapper.findSubmission(requestId);
        if (request == null || account.employeeId() != request.employeeId()) throw new IllegalArgumentException("Overtime request is not available");
        return request;
    }

    private UserAccount employeeAccount(long userId) {
        UserAccount account = userAccountMapper.findById(userId);
        if (account == null || account.employeeId() == null || !isEligibleEmployee(account.employeeId())) {
            throw new IllegalArgumentException("Employee cannot create overtime requests");
        }
        return account;
    }

    private boolean isEligibleEmployee(long employeeId) {
        String status = employeeAttendanceMapper.findEmploymentStatus(employeeId);
        return "FORMAL".equals(status) || "PROBATION".equals(status);
    }
}
