package com.hrpm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpm.common.IdGenerator;
import com.hrpm.common.TraceIdContext;
import com.hrpm.common.exception.DataScopeDeniedException;
import com.hrpm.common.exception.DuplicateResourceException;
import com.hrpm.common.exception.OrganizationReferenceInvalidException;
import com.hrpm.common.exception.ResourceNotFoundException;
import com.hrpm.common.exception.VersionConflictException;
import com.hrpm.common.exception.WorkflowTemplateMissingException;
import com.hrpm.dto.PersonnelChangeDTOs.ChangeAction;
import com.hrpm.dto.PersonnelChangeDTOs.ChangeAssignment;
import com.hrpm.dto.PersonnelChangeDTOs.ConfirmHandoverItem;
import com.hrpm.dto.PersonnelChangeDTOs.CreateChange;
import com.hrpm.dto.PersonnelChangeDTOs.CreateHandoverItem;
import com.hrpm.dto.PersonnelChangeDTOs.UpdateChange;
import com.hrpm.entity.Department;
import com.hrpm.entity.Employee;
import com.hrpm.entity.EmployeeDataScope;
import com.hrpm.entity.EmployeeHistory;
import com.hrpm.entity.ExitHandover;
import com.hrpm.entity.ExitHandoverItem;
import com.hrpm.entity.PersonnelChange;
import com.hrpm.entity.PersonnelChangeStatus;
import com.hrpm.entity.PersonnelChangeType;
import com.hrpm.entity.WorkflowInstance;
import com.hrpm.entity.WorkflowInstanceSnapshot;
import com.hrpm.entity.WorkflowNodeSnapshot;
import com.hrpm.entity.WorkflowTemplate;
import com.hrpm.entity.WorkflowTemplateNode;
import com.hrpm.entity.UserAccount;
import com.hrpm.mapper.DepartmentMapper;
import com.hrpm.mapper.EmployeeMapper;
import com.hrpm.mapper.PersonnelChangeMapper;
import com.hrpm.mapper.PositionMapper;
import com.hrpm.mapper.RankMapper;
import com.hrpm.mapper.UserAccountMapper;
import com.hrpm.mapper.UserPermissionMapper;
import com.hrpm.mapper.WorkflowMapper;
import com.hrpm.vo.PageVO;
import com.hrpm.vo.PersonnelChangeVOs.EmployeeHistoryVO;
import com.hrpm.vo.PersonnelChangeVOs.ExitHandoverItemVO;
import com.hrpm.vo.PersonnelChangeVOs.PersonnelChangeDetailVO;
import com.hrpm.vo.PersonnelChangeVOs.PersonnelChangeListItemVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PersonnelChangeService {
    private static final String BUSINESS_TYPE = "PERSONNEL_CHANGE";

    private final PersonnelChangeMapper personnelChangeMapper;
    private final WorkflowMapper workflowMapper;
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final PositionMapper positionMapper;
    private final RankMapper rankMapper;
    private final UserAccountMapper userAccountMapper;
    private final UserPermissionMapper userPermissionMapper;
    private final OrganizationAccessService organizationAccessService;
    private final EmployeeDataScopeResolver employeeDataScopeResolver;
    private final WorkflowApproverResolver workflowApproverResolver;
    private final EmployeeService employeeService;
    private final OperationAuditService operationAuditService;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public PersonnelChangeService(PersonnelChangeMapper personnelChangeMapper, WorkflowMapper workflowMapper,
                                  EmployeeMapper employeeMapper, DepartmentMapper departmentMapper,
                                  PositionMapper positionMapper, RankMapper rankMapper,
                                  UserAccountMapper userAccountMapper, UserPermissionMapper userPermissionMapper,
                                  OrganizationAccessService organizationAccessService,
                                  EmployeeDataScopeResolver employeeDataScopeResolver,
                                  WorkflowApproverResolver workflowApproverResolver,
                                  EmployeeService employeeService, OperationAuditService operationAuditService,
                                  IdGenerator idGenerator, ObjectMapper objectMapper) {
        this.personnelChangeMapper = personnelChangeMapper;
        this.workflowMapper = workflowMapper;
        this.employeeMapper = employeeMapper;
        this.departmentMapper = departmentMapper;
        this.positionMapper = positionMapper;
        this.rankMapper = rankMapper;
        this.userAccountMapper = userAccountMapper;
        this.userPermissionMapper = userPermissionMapper;
        this.organizationAccessService = organizationAccessService;
        this.employeeDataScopeResolver = employeeDataScopeResolver;
        this.workflowApproverResolver = workflowApproverResolver;
        this.employeeService = employeeService;
        this.operationAuditService = operationAuditService;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    public PageVO<PersonnelChangeListItemVO> list(long userId, int page, int pageSize, Long employeeId,
                                                  String changeType, String status,
                                                  LocalDate fromDate, LocalDate toDate) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new OrganizationReferenceInvalidException("Invalid pagination");
        }
        EmployeeDataScope scope = employeeDataScopeResolver.resolve(userId);
        boolean manage = hasPermission(userId, "personnel:manage");
        if (!scope.unrestricted() && scope.employeeIds().isEmpty() && scope.departmentIds().isEmpty() && !manage) {
            return new PageVO<>(List.of(), 0, page, pageSize);
        }
        List<PersonnelChangeListItemVO> records = personnelChangeMapper.findPage(userId, employeeId, changeType, status, fromDate, toDate,
                        scope.unrestricted() || manage, scope.employeeIds(), scope.departmentIds(), (page - 1) * pageSize, pageSize)
                .stream()
                .map(change -> PersonnelChangeListItemVO.from(change, resolveEmployeeName(change)))
                .toList();
        long total = personnelChangeMapper.count(userId, employeeId, changeType, status, fromDate, toDate,
                scope.unrestricted() || manage, scope.employeeIds(), scope.departmentIds());
        return new PageVO<>(records, total, page, pageSize);
    }

    @Transactional
    public PersonnelChangeDetailVO create(long userId, CreateChange command) {
        PersonnelChangeType changeType = parseChangeType(command.changeType());
        Employee current = requireCurrentEmployeeForCreate(userId, changeType, command.employeeId());
        PersonnelSnapshot beforeSnapshot = current == null ? null : PersonnelSnapshot.from(current);
        PersonnelSnapshot afterSnapshot = buildAfterSnapshot(current, command.afterAssignment(), changeType, command.effectiveDate());
        validateAfterSnapshot(userId, current, changeType, afterSnapshot);
        long changeId = idGenerator.nextId();
        String changeNo = "PC-" + changeId;
        if (personnelChangeMapper.insertChange(changeId, changeNo, current == null ? null : current.id(), changeType.name(),
                LocalDate.now(ZoneOffset.UTC), command.effectiveDate(), command.reason().trim(),
                beforeSnapshot == null ? null : toJson(beforeSnapshot), toJson(afterSnapshot),
                null, PersonnelChangeStatus.DRAFT.name(), userId, userId) != 1) {
            throw new IllegalStateException("Unable to create personnel change");
        }
        if (changeType == PersonnelChangeType.TERMINATION) {
            personnelChangeMapper.insertExitHandover(idGenerator.nextId(), changeId, current.id(), "PENDING", userId, userId);
        }
        return detail(userId, changeId);
    }

    @Transactional
    public PersonnelChangeDetailVO update(long userId, long id, UpdateChange command) {
        PersonnelChange change = requireOwnedOrManagedChange(userId, id);
        if (!PersonnelChangeStatus.DRAFT.name().equals(change.status())) {
            throw new IllegalStateException("Personnel change can only be edited in draft");
        }
        if (change.version() != parseVersion(command.version())) {
            throw new VersionConflictException();
        }
        PersonnelChangeType changeType = parseChangeType(command.changeType());
        Employee current = requireCurrentEmployeeForCreate(userId, changeType, command.employeeId());
        PersonnelSnapshot beforeSnapshot = current == null ? null : PersonnelSnapshot.from(current);
        PersonnelSnapshot afterSnapshot = buildAfterSnapshot(current, command.afterAssignment(), changeType, command.effectiveDate());
        validateAfterSnapshot(userId, current, changeType, afterSnapshot);
        if (personnelChangeMapper.updateDraft(id, current == null ? null : current.id(), changeType.name(),
                command.effectiveDate(), command.reason().trim(),
                beforeSnapshot == null ? null : toJson(beforeSnapshot), toJson(afterSnapshot), userId, change.version()) != 1) {
            throw new VersionConflictException();
        }
        if (changeType == PersonnelChangeType.TERMINATION && personnelChangeMapper.findExitHandoverByChangeId(id) == null) {
            personnelChangeMapper.insertExitHandover(idGenerator.nextId(), id, current.id(), "PENDING", userId, userId);
        }
        return detail(userId, id);
    }

    @Transactional
    public PersonnelChangeDetailVO submit(long userId, long id, ChangeAction command) {
        PersonnelChange change = requireOwnedOrManagedChange(userId, id);
        if (!PersonnelChangeStatus.DRAFT.name().equals(change.status())) {
            throw new IllegalStateException("Personnel change is not submittable");
        }
        if (change.version() != parseVersion(command.version())) {
            throw new VersionConflictException();
        }
        PersonnelSnapshot afterSnapshot = parseSnapshot(change.afterSnapshot());
        long targetDepartmentId = requireDepartment(afterSnapshot.departmentId()).id();
        WorkflowTemplate template = workflowMapper.findTemplateForBusiness(BUSINESS_TYPE, targetDepartmentId);
        if (template == null) {
            throw new WorkflowTemplateMissingException();
        }
        long initiatorEmployeeId = resolveWorkflowInitiatorEmployeeId(userId, change);
        WorkflowInstance instance = change.workflowInstanceId() == null ? null : workflowMapper.findInstance(change.workflowInstanceId());
        if (instance != null && "RETURNED".equals(instance.status()) && instance.initiatorUserId() == userId) {
            WorkflowInstanceSnapshot snapshot = parseWorkflowSnapshot(workflowMapper.findInstanceSnapshot(instance.id()));
            WorkflowNodeSnapshot firstNode = firstNode(snapshot.nodes());
            if (workflowMapper.resumeReturnedInstance(instance.id(), firstNode.nodeNo()) != 1) {
                throw new IllegalStateException("Workflow instance changed before resubmission");
            }
            workflowMapper.insertTask(idGenerator.nextId(), instance.id(), firstNode.nodeNo(), toJson(firstNode), firstNode.assigneeUserId());
            if (personnelChangeMapper.updateStatus(change.id(), PersonnelChangeStatus.DRAFT.name(), PersonnelChangeStatus.IN_PROGRESS.name(),
                    instance.id(), userId, change.version()) != 1) {
                throw new VersionConflictException();
            }
            workflowMapper.insertActionLog(idGenerator.nextId(), instance.id(), null, userId, "RESUBMIT", "Personnel change resubmitted");
            return detail(userId, id);
        }
        List<WorkflowNodeSnapshot> nodes = workflowMapper.findNodes(template.id()).stream()
                .map(node -> resolveNode(node, change, initiatorEmployeeId, targetDepartmentId))
                .toList();
        WorkflowNodeSnapshot firstNode = firstNode(nodes);
        WorkflowInstanceSnapshot snapshot = new WorkflowInstanceSnapshot(template.id(), template.templateVersion(),
                initiatorEmployeeId, targetDepartmentId, nodes);
        long instanceId = idGenerator.nextId();
        workflowMapper.insertBusinessInstance(instanceId, BUSINESS_TYPE, change.id(), userId, toJson(snapshot), firstNode.nodeNo());
        workflowMapper.insertTask(idGenerator.nextId(), instanceId, firstNode.nodeNo(), toJson(firstNode), firstNode.assigneeUserId());
        if (personnelChangeMapper.updateStatus(change.id(), PersonnelChangeStatus.DRAFT.name(), PersonnelChangeStatus.IN_PROGRESS.name(),
                instanceId, userId, change.version()) != 1) {
            throw new VersionConflictException();
        }
        workflowMapper.insertActionLog(idGenerator.nextId(), instanceId, null, userId, "SUBMIT", "Personnel change submitted");
        return detail(userId, id);
    }

    @Transactional
    public PersonnelChangeDetailVO withdraw(long userId, long id, ChangeAction command) {
        PersonnelChange change = requireChange(id);
        if (change.createdBy() == null || change.createdBy() != userId) {
            throw new DataScopeDeniedException();
        }
        if (change.version() != parseVersion(command.version())) {
            throw new VersionConflictException();
        }
        if (!PersonnelChangeStatus.IN_PROGRESS.name().equals(change.status()) || change.workflowInstanceId() == null) {
            throw new IllegalStateException("Personnel change is not withdrawable");
        }
        WorkflowInstance instance = workflowMapper.findInstance(change.workflowInstanceId());
        if (instance == null || !"IN_PROGRESS".equals(instance.status()) || instance.initiatorUserId() != userId) {
            throw new IllegalStateException("Workflow instance is not withdrawable");
        }
        Long taskId = workflowMapper.findPendingTaskId(instance.id());
        if (workflowMapper.withdrawPendingTasks(instance.id()) != 1) {
            throw new IllegalStateException("Workflow task changed before withdrawal");
        }
        markWithdrawn(id, userId);
        if (workflowMapper.withdrawInstance(instance.id(), userId, instance.version()) != 1) {
            throw new IllegalStateException("Workflow instance changed before withdrawal");
        }
        workflowMapper.insertActionLog(idGenerator.nextId(), instance.id(), taskId, userId, "WITHDRAW", "Personnel change withdrawn");
        return detail(userId, id);
    }

    @Transactional
    public PersonnelChangeDetailVO effective(long userId, long id, ChangeAction command) {
        PersonnelChange change = requireChange(id);
        if (change.version() != parseVersion(command.version())) {
            throw new VersionConflictException();
        }
        performEffective(change, userId);
        return detail(userId, id);
    }

    public List<EmployeeHistoryVO> history(long userId, long employeeId) {
        Employee employee = employeeMapper.findById(employeeId);
        if (employee == null) {
            throw new ResourceNotFoundException("Employee not found");
        }
        organizationAccessService.requireReadableEmployee(userId, employee);
        return personnelChangeMapper.listEmployeeHistory(employeeId).stream()
                .map(history -> EmployeeHistoryVO.from(history, parseJsonNode(history.snapshot())))
                .toList();
    }

    @Transactional
    public PersonnelChangeDetailVO addHandoverItem(long userId, long id, CreateHandoverItem command) {
        PersonnelChange change = requireOwnedOrManagedChange(userId, id);
        if (parseChangeType(change.changeType()) != PersonnelChangeType.TERMINATION) {
            throw new IllegalStateException("Only termination changes support handover items");
        }
        if (!PersonnelChangeStatus.DRAFT.name().equals(change.status()) && !PersonnelChangeStatus.IN_PROGRESS.name().equals(change.status())) {
            throw new IllegalStateException("Handover items can only be edited before approval");
        }
        ExitHandover handover = requireExitHandover(change.id());
        Long receiverEmployeeId = parseNullableId(command.receiverEmployeeId(), "Invalid handover receiver");
        if (receiverEmployeeId != null) {
            Employee receiver = employeeMapper.findById(receiverEmployeeId);
            if (receiver == null || "TERMINATED".equals(receiver.employmentStatus())) {
                throw new OrganizationReferenceInvalidException("Receiver employee is missing or inactive");
            }
            organizationAccessService.requireWritableEmployee(userId, receiver);
        }
        personnelChangeMapper.insertExitHandoverItem(idGenerator.nextId(), handover.id(), command.itemType().trim(),
                receiverEmployeeId, command.required() == null || command.required(), "PENDING",
                null, null, command.remark(), userId, userId);
        personnelChangeMapper.updateExitHandoverStatus(handover.id(), "IN_PROGRESS", userId);
        return detail(userId, id);
    }

    @Transactional
    public PersonnelChangeDetailVO confirmHandoverItem(long userId, long id, long itemId, ConfirmHandoverItem command) {
        PersonnelChange change = requireReadableChange(userId, id);
        ExitHandover handover = requireExitHandover(change.id());
        ExitHandoverItem item = personnelChangeMapper.findExitHandoverItemById(itemId);
        if (item == null || item.handoverId() != handover.id()) {
            throw new ResourceNotFoundException("Exit handover item not found");
        }
        if (!hasPermission(userId, "personnel:manage")) {
            if (item.receiverEmployeeId() == null) {
                throw new DataScopeDeniedException();
            }
            UserAccount account = userAccountMapper.findById(userId);
            if (account == null || account.employeeId() == null || !account.employeeId().equals(item.receiverEmployeeId())) {
                throw new DataScopeDeniedException();
            }
        }
        if (item.version() != parseVersion(command.version())) {
            throw new VersionConflictException();
        }
        if (personnelChangeMapper.confirmExitHandoverItem(itemId, userId, command.remark(), item.version()) != 1) {
            throw new VersionConflictException();
        }
        personnelChangeMapper.updateExitHandoverStatus(handover.id(),
                personnelChangeMapper.countPendingRequiredExitHandoverItems(handover.id()) == 0 ? "COMPLETED" : "IN_PROGRESS", userId);
        return detail(userId, id);
    }

    public void markApproved(long changeId, long actorUserId) {
        PersonnelChange current = requireChange(changeId);
        if (!PersonnelChangeStatus.IN_PROGRESS.name().equals(current.status())) {
            throw new IllegalStateException("Personnel change is not in progress");
        }
        if (personnelChangeMapper.updateStatus(changeId, PersonnelChangeStatus.IN_PROGRESS.name(), PersonnelChangeStatus.APPROVED.name(),
                null, actorUserId, current.version()) != 1) {
            throw new IllegalStateException("Personnel change changed before approval");
        }
        PersonnelChange approved = requireChange(changeId);
        if (!approved.effectiveDate().isAfter(LocalDate.now(ZoneOffset.UTC))) {
            performEffective(approved, actorUserId);
        }
    }

    public void markRejected(long changeId, long actorUserId) {
        PersonnelChange current = requireChange(changeId);
        if (!PersonnelChangeStatus.IN_PROGRESS.name().equals(current.status())) {
            throw new IllegalStateException("Personnel change is not in progress");
        }
        if (personnelChangeMapper.updateStatus(changeId, PersonnelChangeStatus.IN_PROGRESS.name(), PersonnelChangeStatus.REJECTED.name(),
                null, actorUserId, current.version()) != 1) {
            throw new IllegalStateException("Personnel change changed before rejection");
        }
    }

    public void markWithdrawn(long changeId, long actorUserId) {
        PersonnelChange current = requireChange(changeId);
        if (!PersonnelChangeStatus.IN_PROGRESS.name().equals(current.status())) {
            throw new IllegalStateException("Personnel change is not in progress");
        }
        if (personnelChangeMapper.updateStatus(changeId, PersonnelChangeStatus.IN_PROGRESS.name(), PersonnelChangeStatus.WITHDRAWN.name(),
                null, actorUserId, current.version()) != 1) {
            throw new IllegalStateException("Personnel change changed before withdrawal");
        }
    }

    public void returnToDraft(long changeId, long actorUserId) {
        PersonnelChange current = requireChange(changeId);
        if (!PersonnelChangeStatus.IN_PROGRESS.name().equals(current.status())) {
            throw new IllegalStateException("Personnel change is not in progress");
        }
        if (personnelChangeMapper.updateStatus(changeId, PersonnelChangeStatus.IN_PROGRESS.name(), PersonnelChangeStatus.DRAFT.name(),
                null, actorUserId, current.version()) != 1) {
            throw new IllegalStateException("Personnel change changed before return");
        }
    }

    public PersonnelChangeDetailVO detail(long userId, long id) {
        PersonnelChange change = requireReadableChange(userId, id);
        return toDetailVO(userId, change);
    }

    private PersonnelChangeDetailVO toDetailVO(long userId, PersonnelChange change) {
        List<ExitHandoverItemVO> handoverItems = List.of();
        if (parseChangeType(change.changeType()) == PersonnelChangeType.TERMINATION) {
            ExitHandover handover = personnelChangeMapper.findExitHandoverByChangeId(change.id());
            if (handover != null) {
                handoverItems = personnelChangeMapper.listExitHandoverItems(handover.id()).stream().map(ExitHandoverItemVO::from).toList();
            }
        }
        boolean manage = hasPermission(userId, "personnel:manage");
        return new PersonnelChangeDetailVO(Long.toString(change.id()), change.changeNo(),
                change.employeeId() == null ? null : Long.toString(change.employeeId()), change.changeType(),
                change.applicationDate(), change.effectiveDate(), change.reason(),
                change.beforeSnapshot() == null ? null : parseJsonNode(change.beforeSnapshot()),
                parseJsonNode(change.afterSnapshot()),
                change.workflowInstanceId() == null ? null : Long.toString(change.workflowInstanceId()),
                change.status(), change.createdBy() == null ? null : Long.toString(change.createdBy()),
                change.createdTime(), Integer.toString(change.version()), handoverItems,
                PersonnelChangeStatus.DRAFT.name().equals(change.status()) && (manage || change.createdBy() == userId),
                PersonnelChangeStatus.DRAFT.name().equals(change.status()) && (manage || change.createdBy() == userId),
                PersonnelChangeStatus.IN_PROGRESS.name().equals(change.status()) && change.createdBy() != null && change.createdBy() == userId,
                parseChangeType(change.changeType()) == PersonnelChangeType.TERMINATION
                        && (PersonnelChangeStatus.DRAFT.name().equals(change.status()) || PersonnelChangeStatus.IN_PROGRESS.name().equals(change.status()))
                        && (manage || change.createdBy() == userId));
    }

    private PersonnelChange requireOwnedOrManagedChange(long userId, long id) {
        PersonnelChange change = requireChange(id);
        if (hasPermission(userId, "personnel:manage")) {
            if (change.employeeId() != null) {
                Employee employee = employeeMapper.findById(change.employeeId());
                if (employee != null) {
                    organizationAccessService.requireWritableEmployee(userId, employee);
                }
            }
            return change;
        }
        if (change.createdBy() == null || change.createdBy() != userId) {
            throw new DataScopeDeniedException();
        }
        if (change.employeeId() != null) {
            Employee employee = employeeMapper.findById(change.employeeId());
            if (employee != null) {
                organizationAccessService.requireWritableEmployee(userId, employee);
            }
        }
        return change;
    }

    private PersonnelChange requireReadableChange(long userId, long id) {
        PersonnelChange change = requireChange(id);
        if (change.employeeId() == null) {
            if (hasPermission(userId, "personnel:manage") || (change.createdBy() != null && change.createdBy() == userId)) {
                return change;
            }
            throw new ResourceNotFoundException("Personnel change not found");
        }
        Employee employee = employeeMapper.findById(change.employeeId());
        if (employee == null) {
            throw new ResourceNotFoundException("Personnel change employee not found");
        }
        organizationAccessService.requireReadableEmployee(userId, employee);
        return change;
    }

    private PersonnelChange requireChange(long id) {
        PersonnelChange change = personnelChangeMapper.findById(id);
        if (change == null) {
            throw new ResourceNotFoundException("Personnel change not found");
        }
        return change;
    }

    private Employee requireCurrentEmployeeForCreate(long userId, PersonnelChangeType changeType, String employeeIdText) {
        if (changeType == PersonnelChangeType.ONBOARD) {
            if (employeeIdText != null && !employeeIdText.isBlank()) {
                throw new OrganizationReferenceInvalidException("Onboard change cannot reference an existing employee");
            }
            return null;
        }
        long employeeId = parseRequiredId(employeeIdText, "Invalid employee ID");
        Employee employee = employeeMapper.findById(employeeId);
        if (employee == null) {
            throw new ResourceNotFoundException("Employee not found");
        }
        organizationAccessService.requireWritableEmployee(userId, employee);
        return employee;
    }

    private PersonnelSnapshot buildAfterSnapshot(Employee current, ChangeAssignment assignment, PersonnelChangeType changeType,
                                                 LocalDate effectiveDate) {
        PersonnelSnapshot base = current == null ? null : PersonnelSnapshot.from(current);
        String employeeNo = assignment.employeeNo() == null || assignment.employeeNo().isBlank() ? base == null ? null : base.employeeNo() : assignment.employeeNo().trim();
        String name = assignment.name() == null || assignment.name().isBlank() ? base == null ? null : base.name() : assignment.name().trim();
        String gender = assignment.gender() == null ? base == null ? null : base.gender() : assignment.gender();
        Long departmentId = parseOptionalId(assignment.departmentId());
        if (departmentId == null && base != null) {
            departmentId = base.departmentId();
        }
        Long positionId = parseOptionalId(assignment.positionId());
        if (positionId == null && base != null) {
            positionId = base.positionId();
        }
        Long rankId = parseOptionalId(assignment.rankId());
        if (rankId == null && base != null) {
            rankId = base.rankId();
        }
        Long managerEmployeeId = parseOptionalId(assignment.managerEmployeeId());
        if (managerEmployeeId == null && base != null) {
            managerEmployeeId = base.managerEmployeeId();
        }
        String employmentStatus = assignment.employmentStatus() == null || assignment.employmentStatus().isBlank()
                ? defaultEmploymentStatus(base, changeType)
                : normalizeEmploymentStatus(assignment.employmentStatus());
        LocalDate hireDate = assignment.hireDate() != null ? assignment.hireDate() : base == null ? null : base.hireDate();
        LocalDate probationStartDate = assignment.probationStartDate() != null ? assignment.probationStartDate() : base == null ? null : base.probationStartDate();
        LocalDate probationEndDate = assignment.probationEndDate() != null ? assignment.probationEndDate() : base == null ? null : base.probationEndDate();
        LocalDate terminationDate = assignment.terminationDate() != null ? assignment.terminationDate() : base == null ? null : base.terminationDate();
        if (changeType == PersonnelChangeType.TERMINATION && terminationDate == null) {
            terminationDate = effectiveDate;
        }
        return new PersonnelSnapshot(employeeNo, name, gender, departmentId, positionId, rankId, managerEmployeeId,
                employmentStatus, hireDate, probationStartDate, probationEndDate, terminationDate);
    }

    private void validateAfterSnapshot(long userId, Employee current, PersonnelChangeType changeType, PersonnelSnapshot snapshot) {
        if (snapshot.employeeNo() == null || snapshot.employeeNo().isBlank()) {
            throw new OrganizationReferenceInvalidException("Employee number is required");
        }
        if (snapshot.name() == null || snapshot.name().isBlank()) {
            throw new OrganizationReferenceInvalidException("Employee name is required");
        }
        Department department = requireDepartment(snapshot.departmentId());
        organizationAccessService.requireWritableDepartment(userId, department.id());
        requirePosition(snapshot.positionId());
        if (snapshot.rankId() != null) {
            requireRank(snapshot.rankId());
        }
        if (snapshot.managerEmployeeId() != null) {
            if (current != null && snapshot.managerEmployeeId().equals(current.id())) {
                throw new OrganizationReferenceInvalidException("Employee cannot manage themselves");
            }
            Employee manager = employeeMapper.findById(snapshot.managerEmployeeId());
            if (manager == null || "TERMINATED".equals(manager.employmentStatus())) {
                throw new OrganizationReferenceInvalidException("Manager is missing or inactive");
            }
            organizationAccessService.requireWritableEmployee(userId, manager);
        }
        if (snapshot.hireDate() == null) {
            throw new OrganizationReferenceInvalidException("Hire date is required");
        }
        if (changeType == PersonnelChangeType.ONBOARD && employeeMapper.countByEmployeeNo(snapshot.employeeNo()) > 0) {
            throw new DuplicateResourceException("Employee number already exists");
        }
        if (changeType == PersonnelChangeType.CONFIRM && !"FORMAL".equals(snapshot.employmentStatus())) {
            throw new OrganizationReferenceInvalidException("Confirm change must set employment status to FORMAL");
        }
        if (changeType == PersonnelChangeType.TERMINATION && !"TERMINATED".equals(snapshot.employmentStatus())) {
            throw new OrganizationReferenceInvalidException("Termination change must set employment status to TERMINATED");
        }
    }

    private long resolveWorkflowInitiatorEmployeeId(long userId, PersonnelChange change) {
        if (change.employeeId() != null) {
            return change.employeeId();
        }
        UserAccount account = userAccountMapper.findById(userId);
        if (account == null || account.employeeId() == null) {
            throw new WorkflowTemplateMissingException();
        }
        return account.employeeId();
    }

    private WorkflowNodeSnapshot resolveNode(WorkflowTemplateNode node, PersonnelChange change,
                                             long initiatorEmployeeId, long targetDepartmentId) {
        return new WorkflowNodeSnapshot(node.nodeNo(), node.nodeType(), parseJsonNode(node.approverRule()),
                workflowApproverResolver.resolve(node, initiatorEmployeeId, targetDepartmentId));
    }

    private WorkflowNodeSnapshot firstNode(List<WorkflowNodeSnapshot> nodes) {
        if (nodes.isEmpty()) {
            throw new WorkflowTemplateMissingException();
        }
        return nodes.get(0);
    }

    private PersonnelChangeType parseChangeType(String value) {
        try {
            return PersonnelChangeType.valueOf(value);
        } catch (Exception exception) {
            throw new OrganizationReferenceInvalidException("Invalid personnel change type");
        }
    }

    private String normalizeEmploymentStatus(String value) {
        try {
            return com.hrpm.entity.EmploymentStatus.valueOf(value).name();
        } catch (Exception exception) {
            throw new OrganizationReferenceInvalidException("Invalid employment status");
        }
    }

    private String defaultEmploymentStatus(PersonnelSnapshot base, PersonnelChangeType changeType) {
        if (base != null) {
            if (changeType == PersonnelChangeType.CONFIRM) {
                return "FORMAL";
            }
            return base.employmentStatus();
        }
        return switch (changeType) {
            case ONBOARD -> "PROBATION";
            case TERMINATION -> "TERMINATED";
            default -> throw new OrganizationReferenceInvalidException("Employment status is required");
        };
    }

    private Department requireDepartment(Long id) {
        if (id == null) {
            throw new OrganizationReferenceInvalidException("Department is required");
        }
        Department department = departmentMapper.findById(id);
        if (department == null || !"ACTIVE".equals(department.status())) {
            throw new OrganizationReferenceInvalidException("Department is missing or inactive");
        }
        return department;
    }

    private void requirePosition(Long id) {
        if (id == null || positionMapper.findById(id) == null || !"ACTIVE".equals(positionMapper.findById(id).status())) {
            throw new OrganizationReferenceInvalidException("Position is missing or inactive");
        }
    }

    private void requireRank(Long id) {
        var rank = rankMapper.findById(id);
        if (rank == null || !"ACTIVE".equals(rank.status())) {
            throw new OrganizationReferenceInvalidException("Rank is missing or inactive");
        }
    }

    private void performEffective(PersonnelChange change, long actorUserId) {
        if (!PersonnelChangeStatus.APPROVED.name().equals(change.status())) {
            throw new IllegalStateException("Personnel change is not approved");
        }
        if (change.effectiveDate().isAfter(LocalDate.now(ZoneOffset.UTC))) {
            throw new IllegalStateException("Personnel change effective date has not been reached");
        }
        PersonnelSnapshot afterSnapshot = parseSnapshot(change.afterSnapshot());
        validateAfterSnapshot(actorUserId, change.employeeId() == null ? null : employeeMapper.findById(change.employeeId()),
                parseChangeType(change.changeType()), afterSnapshot);
        Long finalEmployeeId = change.employeeId();
        String accountAction = "UNCHANGED";
        if (parseChangeType(change.changeType()) == PersonnelChangeType.TERMINATION) {
            ExitHandover handover = requireExitHandover(change.id());
            if (personnelChangeMapper.countPendingRequiredExitHandoverItems(handover.id()) > 0) {
                throw new IllegalStateException("Required exit handover items are incomplete");
            }
        }
        if (change.employeeId() == null) {
            if (employeeMapper.countByEmployeeNo(afterSnapshot.employeeNo()) > 0) {
                throw new DuplicateResourceException("Employee number already exists");
            }
            long employeeId = idGenerator.nextId();
            employeeMapper.insert(new Employee(employeeId, afterSnapshot.employeeNo(), afterSnapshot.name(), afterSnapshot.gender(),
                    afterSnapshot.departmentId(), null, afterSnapshot.positionId(), null, afterSnapshot.rankId(), null,
                    afterSnapshot.managerEmployeeId(), null, afterSnapshot.employmentStatus(), afterSnapshot.hireDate(),
                    afterSnapshot.probationStartDate(), afterSnapshot.probationEndDate(), 0));
            finalEmployeeId = employeeId;
        } else {
            Employee current = employeeMapper.findById(change.employeeId());
            if (current == null) {
                throw new ResourceNotFoundException("Employee not found");
            }
            organizationAccessService.requireWritableEmployee(actorUserId, current);
            if (employeeMapper.updateAssignment(current.id(), afterSnapshot.employeeNo(), afterSnapshot.name(),
                    afterSnapshot.gender(), afterSnapshot.departmentId(), afterSnapshot.positionId(), afterSnapshot.rankId(),
                    afterSnapshot.managerEmployeeId(), afterSnapshot.employmentStatus(), afterSnapshot.hireDate(),
                    afterSnapshot.probationStartDate(), afterSnapshot.probationEndDate(), afterSnapshot.terminationDate(),
                    actorUserId, current.version()) != 1) {
                throw new VersionConflictException();
            }
        }
        if (personnelChangeMapper.insertHistory(new EmployeeHistory(idGenerator.nextId(), finalEmployeeId, change.id(),
                change.changeType(), change.effectiveDate(), toJson(afterSnapshot), actorUserId, null)) != 1) {
            throw new IllegalStateException("Unable to write employee history");
        }
        if (parseChangeType(change.changeType()) == PersonnelChangeType.TERMINATION) {
            UserAccount account = userAccountMapper.findByEmployeeId(finalEmployeeId);
            if (account == null) {
                accountAction = "NO_LINKED_ACCOUNT";
            } else if ("ACTIVE".equals(account.status())) {
                userAccountMapper.disableForEmployee(finalEmployeeId);
                accountAction = "DISABLED";
            } else if ("DISABLED".equals(account.status())) {
                accountAction = "ALREADY_DISABLED";
            } else {
                accountAction = "NON_ACTIVE_" + account.status();
            }
        } else if (parseChangeType(change.changeType()) == PersonnelChangeType.ONBOARD) {
            employeeService.ensureActiveEmployeeAccount(finalEmployeeId, afterSnapshot.employeeNo());
            accountAction = "ENSURED_ACTIVE";
        }
        if (personnelChangeMapper.markEffective(change.id(), finalEmployeeId, actorUserId, change.version()) != 1) {
            throw new VersionConflictException();
        }
        writeAuditLog(change.id(), actorUserId, "EFFECTIVE", Map.of(
                "changeNo", change.changeNo(),
                "changeType", change.changeType(),
                "employeeId", finalEmployeeId,
                "effectiveDate", change.effectiveDate().toString(),
                "accountAction", accountAction
        ));
    }

    private ExitHandover requireExitHandover(long changeId) {
        ExitHandover handover = personnelChangeMapper.findExitHandoverByChangeId(changeId);
        if (handover == null) {
            throw new ResourceNotFoundException("Exit handover not found");
        }
        return handover;
    }

    private boolean hasPermission(long userId, String permissionCode) {
        return userPermissionMapper.findPermissionCodesByUserId(userId).contains(permissionCode);
    }

    private int parseVersion(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new OrganizationReferenceInvalidException("Invalid version");
        }
    }

    private long parseRequiredId(String value, String message) {
        Long id = parseOptionalId(value);
        if (id == null) {
            throw new OrganizationReferenceInvalidException(message);
        }
        return id;
    }

    private Long parseNullableId(String value, String message) {
        try {
            return parseOptionalId(value);
        } catch (OrganizationReferenceInvalidException exception) {
            throw new OrganizationReferenceInvalidException(message);
        }
    }

    private Long parseOptionalId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new OrganizationReferenceInvalidException("Invalid organization reference");
        }
    }

    private String resolveEmployeeName(PersonnelChange change) {
        if (change.employeeId() != null) {
            Employee employee = employeeMapper.findById(change.employeeId());
            if (employee != null) {
                return employee.name();
            }
        }
        return parseSnapshot(change.afterSnapshot()).name();
    }

    private PersonnelSnapshot parseSnapshot(String value) {
        try {
            return objectMapper.readValue(value, PersonnelSnapshot.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse personnel snapshot", exception);
        }
    }

    private WorkflowInstanceSnapshot parseWorkflowSnapshot(String value) {
        try {
            return objectMapper.readValue(value, WorkflowInstanceSnapshot.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse workflow snapshot", exception);
        }
    }

    private JsonNode parseJsonNode(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse JSON content", exception);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize JSON content", exception);
        }
    }

    private void writeAuditLog(long objectId, long actorUserId, String action, Map<String, Object> summary) {
        operationAuditService.recordSuccess("PERSONNEL", action, "PERSONNEL_CHANGE", objectId, actorUserId, summary);
    }

    private record PersonnelSnapshot(String employeeNo, String name, String gender, Long departmentId, Long positionId,
                                     Long rankId, Long managerEmployeeId, String employmentStatus, LocalDate hireDate,
                                     LocalDate probationStartDate, LocalDate probationEndDate, LocalDate terminationDate) {
        static PersonnelSnapshot from(Employee employee) {
            return new PersonnelSnapshot(employee.employeeNo(), employee.name(), employee.gender(),
                    employee.departmentId(), employee.positionId(), employee.rankId(), employee.managerEmployeeId(),
                    employee.employmentStatus(), employee.hireDate(), employee.probationStartDate(),
                    employee.probationEndDate(), null);
        }
    }
}
