package com.hrpm.service;

import com.hrpm.common.IdGenerator;
import com.hrpm.common.exception.DuplicateResourceException;
import com.hrpm.common.exception.OrganizationReferenceInvalidException;
import com.hrpm.common.exception.ResourceNotFoundException;
import com.hrpm.common.exception.VersionConflictException;
import com.hrpm.dto.DisableLeaveTypeDTO;
import com.hrpm.dto.CreateLeaveTypeDTO;
import com.hrpm.dto.UpdateLeaveTypeDTO;
import com.hrpm.entity.LeaveType;
import com.hrpm.mapper.EmployeeMapper;
import com.hrpm.mapper.LeaveBalanceMapper;
import com.hrpm.mapper.LeaveTypeMapper;
import com.hrpm.vo.LeaveTypeVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveTypeService {
    private final LeaveTypeMapper leaveTypeMapper;
    private final EmployeeMapper employeeMapper;
    private final LeaveBalanceMapper leaveBalanceMapper;
    private final IdGenerator idGenerator;

    public LeaveTypeService(LeaveTypeMapper leaveTypeMapper, EmployeeMapper employeeMapper,
                            LeaveBalanceMapper leaveBalanceMapper, IdGenerator idGenerator) {
        this.leaveTypeMapper = leaveTypeMapper;
        this.employeeMapper = employeeMapper;
        this.leaveBalanceMapper = leaveBalanceMapper;
        this.idGenerator = idGenerator;
    }

    public List<LeaveTypeVO> listActive() {
        return leaveTypeMapper.listActive().stream().map(LeaveTypeVO::from).toList();
    }
    public List<LeaveTypeVO> listAll() { return leaveTypeMapper.listAll().stream().map(LeaveTypeVO::from).toList(); }

    @Transactional
    public LeaveTypeVO create(CreateLeaveTypeDTO request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        validateQuota(request.deductBalance(), request.annualQuota());
        if (leaveTypeMapper.findByCode(code) != null) {
            throw new DuplicateResourceException("Leave type code already exists");
        }
        long id = idGenerator.nextId();
        leaveTypeMapper.insert(id, code, request.name().trim(), request.deductBalance(), request.minUnitHours(), request.annualQuota());
        if (request.deductBalance()) {
            int year = LocalDate.now().getYear();
            for (long employeeId : employeeMapper.listActiveIds()) {
                leaveBalanceMapper.insert(idGenerator.nextId(), employeeId, code, year, request.annualQuota());
            }
        }
        return LeaveTypeVO.from(leaveTypeMapper.findById(id));
    }
    @Transactional
    public LeaveTypeVO update(long id, UpdateLeaveTypeDTO request) {
        LeaveType current = require(id);
        validateQuota(request.deductBalance(), request.annualQuota());
        if (leaveTypeMapper.update(id, request.name().trim(), request.deductBalance(), request.minUnitHours(), request.annualQuota(), parseVersion(request.version())) != 1) throw new VersionConflictException();
        return LeaveTypeVO.from(leaveTypeMapper.findById(id));
    }
    @Transactional
    public LeaveTypeVO disable(long id, DisableLeaveTypeDTO request) {
        require(id);
        if (leaveTypeMapper.disable(id, parseVersion(request.version())) != 1) throw new VersionConflictException();
        return LeaveTypeVO.from(leaveTypeMapper.findById(id));
    }
    private LeaveType require(long id) { LeaveType type = leaveTypeMapper.findById(id); if (type == null) throw new ResourceNotFoundException("Leave type not found"); return type; }
    private int parseVersion(String value) { try { return Integer.parseInt(value); } catch (Exception exception) { throw new OrganizationReferenceInvalidException("Invalid leave type version"); } }

    private void validateQuota(boolean deductBalance, BigDecimal annualQuota) {
        if ((deductBalance && (annualQuota == null || annualQuota.signum() <= 0))
                || (!deductBalance && annualQuota != null)) {
            throw new OrganizationReferenceInvalidException("Annual quota must be positive only for balance-deducting leave types");
        }
    }
}
