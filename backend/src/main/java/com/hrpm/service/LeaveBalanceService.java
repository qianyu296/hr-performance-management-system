package com.hrpm.service;

import com.hrpm.common.IdGenerator;
import com.hrpm.common.exception.OrganizationReferenceInvalidException;
import com.hrpm.common.exception.ResourceNotFoundException;
import com.hrpm.common.exception.VersionConflictException;
import com.hrpm.dto.AdjustLeaveBalanceDTO;
import com.hrpm.entity.LeaveBalance;
import com.hrpm.entity.UserAccount;
import com.hrpm.mapper.LeaveBalanceMapper;
import com.hrpm.mapper.UserAccountMapper;
import com.hrpm.vo.LeaveBalanceChangeVO;
import com.hrpm.vo.LeaveBalanceVO;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveBalanceService {
    private final LeaveBalanceMapper leaveBalanceMapper;
    private final UserAccountMapper userAccountMapper;
    private final IdGenerator idGenerator;

    public LeaveBalanceService(LeaveBalanceMapper leaveBalanceMapper, UserAccountMapper userAccountMapper, IdGenerator idGenerator) {
        this.leaveBalanceMapper = leaveBalanceMapper;
        this.userAccountMapper = userAccountMapper;
        this.idGenerator = idGenerator;
    }

    public List<LeaveBalanceVO> listMine(long userId) {
        UserAccount account = userAccountMapper.findById(userId);
        if (account == null || account.employeeId() == null) {
            throw new OrganizationReferenceInvalidException("Current user is not linked to an employee");
        }
        return listByEmployee(account.employeeId());
    }

    public List<LeaveBalanceVO> listByEmployee(long employeeId) {
        return leaveBalanceMapper.listByEmployeeId(employeeId).stream().map(LeaveBalanceVO::from).toList();
    }

    public List<LeaveBalanceChangeVO> listChanges(long balanceId) {
        requireBalance(balanceId);
        return leaveBalanceMapper.listChanges(balanceId).stream().map(LeaveBalanceChangeVO::from).toList();
    }

    @Transactional
    public LeaveBalanceVO adjust(long actorUserId, long balanceId, AdjustLeaveBalanceDTO request) {
        LeaveBalance balance = requireBalance(balanceId);
        int version = parseVersion(request.version());
        BigDecimal signedDelta = "INCREASE".equals(request.direction()) ? request.deltaHours()
                : "DECREASE".equals(request.direction()) ? request.deltaHours().negate() : null;
        if (signedDelta == null) {
            throw new OrganizationReferenceInvalidException("Invalid balance adjustment direction");
        }
        BigDecimal after = balance.availableHours().add(signedDelta);
        if (after.signum() < 0) {
            throw new OrganizationReferenceInvalidException("Balance cannot be negative");
        }
        if (leaveBalanceMapper.updateAvailableHours(balance.id(), version, after) != 1) {
            throw new VersionConflictException();
        }
        long adjustmentId = idGenerator.nextId();
        leaveBalanceMapper.insertManualAdjustment(idGenerator.nextId(), balance.id(), balance.employeeId(), balance.balanceType(),
                signedDelta, balance.availableHours(), after, adjustmentId, request.reason(), actorUserId);
        LeaveBalance updated = leaveBalanceMapper.findById(balanceId);
        return LeaveBalanceVO.from(updated);
    }

    private LeaveBalance requireBalance(long balanceId) {
        LeaveBalance balance = leaveBalanceMapper.findById(balanceId);
        if (balance == null) {
            throw new ResourceNotFoundException("Leave balance not found");
        }
        return balance;
    }

    private int parseVersion(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new OrganizationReferenceInvalidException("Invalid leave balance version");
        }
    }
}
