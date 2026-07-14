package com.hrpm.service;

import com.hrpm.common.IdGenerator;
import com.hrpm.common.exception.*;
import com.hrpm.dto.CreateEmployeeDTO;
import com.hrpm.dto.UpdateEmployeeDTO;
import com.hrpm.entity.*;
import com.hrpm.mapper.*;
import com.hrpm.vo.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.security.SecureRandom;
import java.util.Locale;

@Service
public class EmployeeService {
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final PositionMapper positionMapper;
    private final RankMapper rankMapper;
    private final EmployeeDataScopeResolver dataScopeResolver;
    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;

    public EmployeeService(EmployeeMapper employeeMapper, DepartmentMapper departmentMapper,
                           PositionMapper positionMapper, RankMapper rankMapper, IdGenerator idGenerator, EmployeeDataScopeResolver dataScopeResolver,
                           UserAccountMapper userAccountMapper, PasswordEncoder passwordEncoder) {
        this.employeeMapper = employeeMapper;
        this.departmentMapper = departmentMapper;
        this.positionMapper = positionMapper;
        this.rankMapper = rankMapper;
        this.idGenerator = idGenerator;
        this.dataScopeResolver = dataScopeResolver;
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public PageVO<EmployeeListVO> list(long userId, int page, int pageSize, String keyword, Long departmentId,
                                       Long positionId, String employmentStatus) {
        if (page < 1 || pageSize < 1 || pageSize > 100) throw new OrganizationReferenceInvalidException("Invalid pagination");
        EmployeeDataScope scope = dataScopeResolver.resolve(userId);
        if (scope.isEmpty()) return new PageVO<>(List.of(), 0, page, pageSize);
        List<EmployeeListVO> records = employeeMapper.findPage(keyword, departmentId, positionId, employmentStatus,
                scope.unrestricted() ? null : scope.employeeIds(), scope.unrestricted() ? null : scope.departmentIds(), (page - 1) * pageSize, pageSize).stream().map(EmployeeListVO::from).toList();
        return new PageVO<>(records, employeeMapper.count(keyword, departmentId, positionId, employmentStatus,
                scope.unrestricted() ? null : scope.employeeIds(), scope.unrestricted() ? null : scope.departmentIds()), page, pageSize);
    }

    public Employee get(long id) {
        Employee employee = employeeMapper.findById(id);
        if (employee == null) throw new ResourceNotFoundException("Employee not found");
        return employee;
    }

    public Employee getForUser(long userId, long id) {
        EmployeeDataScope scope = dataScopeResolver.resolve(userId);
        if (scope.isEmpty()) throw new ResourceNotFoundException("Employee not found");
        Employee employee = get(id);
        if (!scope.unrestricted()
                && !scope.employeeIds().contains(employee.id())
                && !scope.departmentIds().contains(employee.departmentId())) {
            throw new ResourceNotFoundException("Employee not found");
        }
        return employee;
    }

    @Transactional
    public CreatedEmployeeVO create(CreateEmployeeDTO request) {
        if (employeeMapper.countByEmployeeNo(request.employeeNo()) > 0) throw new DuplicateResourceException("Employee number already exists");
        String username = request.employeeNo().trim().toLowerCase(Locale.ROOT);
        if (userAccountMapper.findByUsername(username) != null) throw new DuplicateResourceException("Employee account username already exists");
        long id = idGenerator.nextId();
        References refs = validateReferences(id, request.departmentId(), request.positionId(), request.rankId(), request.managerEmployeeId());
        Employee employee = new Employee(id, request.employeeNo(), request.name(), request.gender(), refs.departmentId(), null,
                refs.positionId(), null, refs.rankId(), null, refs.managerId(), null, request.employmentStatus(), request.hireDate(),
                request.probationStartDate(), request.probationEndDate(), 0);
        employeeMapper.insert(employee);
        String initialPassword = temporaryPassword();
        long userId = idGenerator.nextId();
        userAccountMapper.insertEmployeeAccount(userId, username, passwordEncoder.encode(initialPassword), id);
        if (userAccountMapper.assignEmployeeSelfServiceRole(idGenerator.nextId(), userId) != 1) throw new IllegalStateException("Employee self-service role is unavailable");
        return new CreatedEmployeeVO(EmployeeVO.from(get(id)), username, initialPassword);
    }

    @Transactional
    public Employee update(long id, UpdateEmployeeDTO request) {
        Employee current = get(id);
        References refs = validateReferences(id, request.departmentId(), request.positionId(), request.rankId(), request.managerEmployeeId());
        Employee employee = new Employee(id, current.employeeNo(), request.name(), request.gender(), refs.departmentId(), null,
                refs.positionId(), null, refs.rankId(), null, refs.managerId(), null, current.employmentStatus(), request.hireDate(),
                request.probationStartDate(), request.probationEndDate(), Integer.parseInt(request.version()));
        if (employeeMapper.update(employee) == 0) throw new VersionConflictException();
        return get(id);
    }

    private References validateReferences(long employeeId, String departmentId, String positionId, String rankId, String managerId) {
        long department = Long.parseLong(departmentId);
        Department departmentValue = departmentMapper.findById(department);
        if (departmentValue == null || !"ACTIVE".equals(departmentValue.status())) throw new OrganizationReferenceInvalidException("Department is missing or inactive");
        long position = Long.parseLong(positionId);
        Position positionValue = positionMapper.findById(position);
        if (positionValue == null || !"ACTIVE".equals(positionValue.status())) throw new OrganizationReferenceInvalidException("Position is missing or inactive");
        Long rank = parseNullable(rankId);
        if (rank != null) {
            com.hrpm.entity.Rank rankValue = rankMapper.findById(rank);
            if (rankValue == null || !"ACTIVE".equals(rankValue.status())) throw new OrganizationReferenceInvalidException("Rank is missing or inactive");
        }
        Long manager = parseNullable(managerId);
        if (manager != null) {
            if (manager == employeeId) throw new OrganizationReferenceInvalidException("Employee cannot manage themselves");
            Employee managerValue = employeeMapper.findById(manager);
            if (managerValue == null || "TERMINATED".equals(managerValue.employmentStatus())) throw new OrganizationReferenceInvalidException("Manager is missing or inactive");
        }
        return new References(department, position, rank, manager);
    }

    private Long parseNullable(String value) { return value == null || value.isBlank() ? null : Long.parseLong(value); }
    private String temporaryPassword() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom(); StringBuilder value = new StringBuilder("Hrpm!");
        for (int index = 0; index < 12; index++) value.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return value.toString();
    }
    private record References(long departmentId, long positionId, Long rankId, Long managerId) {}
}
