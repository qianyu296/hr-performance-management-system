package com.hrpm.service;


import com.hrpm.common.IdGenerator;
import com.hrpm.dto.CreateLeaveRequestDTO;
import com.hrpm.entity.LeaveBalanceRow;
import com.hrpm.entity.LeaveRequestListRow;
import com.hrpm.entity.LeaveRequestRecord;
import com.hrpm.entity.LeaveRequestSubmission;
import com.hrpm.entity.LeaveType;
import com.hrpm.entity.UserAccount;
import com.hrpm.mapper.EmployeeAttendanceMapper;
import com.hrpm.mapper.LeaveRequestMapper;
import com.hrpm.mapper.LeaveTypeMapper;
import com.hrpm.mapper.UserAccountMapper;
import com.hrpm.vo.LeaveRequestListVO;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveRequestService {
    private final UserAccountMapper userAccountMapper;
    private final EmployeeAttendanceMapper employeeAttendanceMapper;
    private final LeaveTypeMapper leaveTypeMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final IdGenerator idGenerator;
    private final LeaveWorkflowService leaveWorkflowService;
    private final WorkTimeService workTimeService;
    private final LeaveBalanceProvisioningService leaveBalanceProvisioningService;

    public LeaveRequestService(
            UserAccountMapper userAccountMapper,
            EmployeeAttendanceMapper employeeAttendanceMapper,
            LeaveTypeMapper leaveTypeMapper,
            LeaveRequestMapper leaveRequestMapper,
            IdGenerator idGenerator,
            LeaveWorkflowService leaveWorkflowService,
            WorkTimeService workTimeService,
            LeaveBalanceProvisioningService leaveBalanceProvisioningService) {
        this.userAccountMapper = userAccountMapper;
        this.employeeAttendanceMapper = employeeAttendanceMapper;
        this.leaveTypeMapper = leaveTypeMapper;
        this.leaveRequestMapper = leaveRequestMapper;
        this.idGenerator = idGenerator;
        this.leaveWorkflowService = leaveWorkflowService;
        this.workTimeService = workTimeService;
        this.leaveBalanceProvisioningService = leaveBalanceProvisioningService;
    }

    @Transactional
    public LeaveRequestRecord createDraft(long userId, CreateLeaveRequestDTO command) {
        UserAccount account = userAccountMapper.findById(userId);
        if (account == null || account.employeeId() == null) {
            throw new IllegalArgumentException("Current user is not linked to an employee");
        }
        String employmentStatus = employeeAttendanceMapper.findEmploymentStatus(account.employeeId());
        if (!"FORMAL".equals(employmentStatus) && !"PROBATION".equals(employmentStatus)) {
            throw new IllegalArgumentException("Employee cannot create leave requests");
        }
        LeaveType leaveType = leaveTypeMapper.findById(Long.parseLong(command.leaveTypeId()));
        if (leaveType == null || !"ACTIVE".equals(leaveType.status())) {
            throw new IllegalArgumentException("Leave type is unavailable");
        }
        leaveBalanceProvisioningService.initializeForLeaveType(account.employeeId(), leaveType,
                command.startTime().atZone(java.time.ZoneOffset.UTC).getYear());
        if (!command.endTime().isAfter(command.startTime())) {
            throw new IllegalArgumentException("Leave end time must be after start time");
        }
        BigDecimal durationHours = workTimeService.calculateWorkHours(command.startTime(), command.endTime());
        if (durationHours.signum() <= 0 || durationHours.remainder(leaveType.minUnitHours()).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Leave duration does not match the leave type minimum unit");
        }
        long id = idGenerator.nextId();
        LeaveRequestRecord request = new LeaveRequestRecord(
                id,
                "LR" + id,
                account.employeeId(),
                leaveType.id(),
                command.startTime(),
                command.endTime(),
                durationHours,
                command.reason(),
                "DRAFT",
                "{}");
        leaveRequestMapper.insert(request);
        return request;
    }

    public LeaveRequestRecord submit(long userId, long requestId, int version) {
        UserAccount account = userAccountMapper.findById(userId);
        LeaveRequestSubmission request = leaveRequestMapper.findSubmission(requestId);
        if (account == null || account.employeeId() == null || request == null || account.employeeId() != request.employeeId()) {
            throw new IllegalArgumentException("Leave request is not available");
        }
        leaveWorkflowService.submit(userId, requestWithVersion(request, version));
        return new LeaveRequestRecord(request.id(), "", request.employeeId(), request.leaveTypeId(), request.startTime(), request.endTime(),
                request.durationHours(), "", "IN_PROGRESS", "{}");
    }

    public List<LeaveRequestListVO> listForUser(long userId) {
        UserAccount account = userAccountMapper.findById(userId);
        if (account == null || account.employeeId() == null) {
            throw new IllegalArgumentException("Current user is not linked to an employee");
        }
        return leaveRequestMapper.listByEmployeeId(account.employeeId()).stream().map(LeaveRequestListRow::toItem).toList();
    }

    @Transactional
    public LeaveRequestRecord cancel(long userId, long requestId, int version) {
        UserAccount account = userAccountMapper.findById(userId);
        LeaveRequestSubmission request = leaveRequestMapper.findSubmission(requestId);
        if (account == null || account.employeeId() == null || request == null || account.employeeId() != request.employeeId()) {
            throw new IllegalArgumentException("Leave request is not available");
        }
        if (!"APPROVED".equals(request.status()) || request.version() != version) {
            throw new IllegalStateException("Leave request is not cancellable");
        }
        LeaveBalanceRow balance = leaveRequestMapper.findBalance(request.employeeId(), request.balanceType(), request.startTime().atZone(java.time.ZoneOffset.UTC).getYear());
        if (balance == null || leaveRequestMapper.cancelRequest(request.id(), request.employeeId(), version) != 1) {
            throw new IllegalStateException("Leave request changed before cancellation");
        }
        BigDecimal after = balance.availableHours().add(request.durationHours());
        if (leaveRequestMapper.updateBalance(balance.id(), balance.version(), after) != 1) {
            throw new IllegalStateException("Leave balance changed before cancellation");
        }
        leaveRequestMapper.insertCancellationBalanceChange(idGenerator.nextId(), balance.id(), request.employeeId(), request.balanceType(),
                request.durationHours(), balance.availableHours(), after, request.id());
        return new LeaveRequestRecord(request.id(), "", request.employeeId(), request.leaveTypeId(), request.startTime(), request.endTime(),
                request.durationHours(), "", "CANCELLED", "{}");
    }

    private LeaveRequestSubmission requestWithVersion(LeaveRequestSubmission request, int version) {
        if (request.version() != version) {
            throw new IllegalStateException("Leave request version is stale");
        }
        return request;
    }
}
