package com.hrpm.service;

import com.hrpm.common.IdGenerator;
import com.hrpm.common.exception.OrganizationReferenceInvalidException;
import com.hrpm.entity.LeaveType;
import com.hrpm.mapper.LeaveBalanceMapper;
import com.hrpm.mapper.LeaveTypeMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LeaveBalanceProvisioningService {
    private final LeaveTypeMapper leaveTypeMapper;
    private final LeaveBalanceMapper leaveBalanceMapper;
    private final IdGenerator idGenerator;

    public LeaveBalanceProvisioningService(LeaveTypeMapper leaveTypeMapper, LeaveBalanceMapper leaveBalanceMapper,
                                           IdGenerator idGenerator) {
        this.leaveTypeMapper = leaveTypeMapper;
        this.leaveBalanceMapper = leaveBalanceMapper;
        this.idGenerator = idGenerator;
    }

    public void initializeForEmployee(long employeeId, int year) {
        for (LeaveType leaveType : leaveTypeMapper.listActive()) {
            initializeForLeaveType(employeeId, leaveType, year);
        }
    }

    public void initializeForLeaveType(long employeeId, LeaveType leaveType, int year) {
        if (!leaveType.deductBalance()) {
            return;
        }
        BigDecimal annualQuota = leaveType.annualQuota();
        if (annualQuota == null || annualQuota.signum() <= 0) {
            throw new OrganizationReferenceInvalidException("Leave type annual quota is not configured");
        }
        leaveBalanceMapper.insert(idGenerator.nextId(), employeeId, leaveType.code(), year, annualQuota);
    }
}
