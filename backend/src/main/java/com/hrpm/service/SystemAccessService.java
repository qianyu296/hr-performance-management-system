package com.hrpm.service;

import com.hrpm.common.IdGenerator;
import com.hrpm.common.exception.DuplicateResourceException;
import com.hrpm.common.exception.OrganizationReferenceInvalidException;
import com.hrpm.common.exception.ResourceNotFoundException;
import com.hrpm.common.exception.VersionConflictException;
import com.hrpm.dto.CreateSystemRoleDTO;
import com.hrpm.dto.UpdateSystemRoleDTO;
import com.hrpm.dto.UpdateUserRolesDTO;
import com.hrpm.entity.Department;
import com.hrpm.entity.RoleDataScope;
import com.hrpm.entity.SystemMenu;
import com.hrpm.entity.SystemRole;
import com.hrpm.entity.SystemUser;
import com.hrpm.mapper.SystemAccessMapper;
import com.hrpm.vo.DepartmentVO;
import com.hrpm.vo.PageVO;
import com.hrpm.vo.SystemMenuVO;
import com.hrpm.vo.SystemRoleDetailVO;
import com.hrpm.vo.SystemRoleVO;
import com.hrpm.vo.SystemUserVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemAccessService {
    private static final Set<String> ROLE_STATUSES = Set.of("ACTIVE", "INACTIVE");
    private static final Set<String> DATA_SCOPE_TYPES = Set.of("SELF", "DIRECT", "DEPT", "DEPT_TREE", "ALL", "CUSTOM");
    private static final Pattern ROLE_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,63}$");

    private final SystemAccessMapper mapper;
    private final IdGenerator idGenerator;

    public SystemAccessService(SystemAccessMapper mapper, IdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    public List<SystemRoleVO> listRoles() {
        return mapper.findRoles().stream().map(SystemRoleVO::from).toList();
    }

    public SystemRoleDetailVO getRole(long roleId) {
        return toRoleDetail(requireRole(roleId));
    }

    public List<SystemMenuVO> listMenus() {
        return mapper.findMenus().stream().map(SystemMenuVO::from).toList();
    }

    public List<DepartmentVO> listDepartments() {
        Map<Long, DepartmentVO> nodes = new LinkedHashMap<>();
        for (Department department : mapper.findAllDepartments()) {
            nodes.put(department.id(), DepartmentVO.from(department));
        }
        List<DepartmentVO> roots = new ArrayList<>();
        for (DepartmentVO node : nodes.values()) {
            if (node.parentId() == null) {
                roots.add(node);
                continue;
            }
            DepartmentVO parent = nodes.get(Long.parseLong(node.parentId()));
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        return roots;
    }

    public PageVO<SystemUserVO> listUsers(int page, int pageSize) {
        validatePage(page, pageSize);
        List<SystemUserVO> records = mapper.findUsers((page - 1) * pageSize, pageSize).stream()
                .map(user -> SystemUserVO.from(user, roleIds(user.id())))
                .toList();
        return new PageVO<>(records, mapper.countUsers(), page, pageSize);
    }

    @Transactional
    public SystemRoleDetailVO createRole(CreateSystemRoleDTO request) {
        String code = normalizeRoleCode(request.code());
        validateRolePayload(code, request.status(), request.dataScopeType(), request.menuIds(), request.departmentIds());
        if (mapper.findRoleByCode(code) != null) {
            throw new DuplicateResourceException("Role code already exists");
        }
        long roleId = idGenerator.nextId();
        mapper.insertRole(roleId, code, request.name().trim(), request.status());
        replaceRoleConfig(roleId, request.menuIds(), request.dataScopeType(), request.departmentIds());
        return getRole(roleId);
    }

    @Transactional
    public SystemRoleDetailVO updateRole(long roleId, UpdateSystemRoleDTO request) {
        SystemRole role = requireRole(roleId);
        validateRolePayload(role.code(), request.status(), request.dataScopeType(), request.menuIds(), request.departmentIds());
        int version = parseVersion(request.version());
        if (mapper.updateRole(roleId, request.name().trim(), request.status(), version) != 1) {
            throw new VersionConflictException();
        }
        replaceRoleConfig(roleId, request.menuIds(), request.dataScopeType(), request.departmentIds());
        return getRole(roleId);
    }

    @Transactional
    public SystemUserVO updateUserRoles(long userId, UpdateUserRolesDTO request) {
        SystemUser user = mapper.findUserById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        int version = parseVersion(request.version());
        Set<Long> roleIds = parseRoleIds(request.roleIds());
        for (long roleId : roleIds) {
            if (mapper.findActiveRoleById(roleId) == null) {
                throw new OrganizationReferenceInvalidException("Role is missing or inactive");
            }
        }
        mapper.deleteRolesForUser(userId);
        for (long roleId : roleIds) {
            mapper.insertUserRole(idGenerator.nextId(), userId, roleId);
        }
        if (mapper.updateRolesVersionAndRevokeSessions(userId, version) != 1) {
            throw new VersionConflictException();
        }
        SystemUser updated = mapper.findUserById(userId);
        return SystemUserVO.from(updated, roleIds(updated.id()));
    }

    private void replaceRoleConfig(long roleId, List<String> menuIdValues, String dataScopeTypeValue, List<String> departmentIdValues) {
        mapper.deleteRoleMenus(roleId);
        Set<Long> menuIds = parseIdSet(menuIdValues, "Invalid menu ID");
        for (long menuId : menuIds) {
            mapper.insertRoleMenu(idGenerator.nextId(), roleId, menuId);
        }

        for (long scopeId : mapper.findScopeIdsByRoleId(roleId)) {
            mapper.deleteScopeDepartments(scopeId);
        }
        mapper.deleteRoleScopes(roleId);

        String dataScopeType = normalizeDataScopeType(dataScopeTypeValue);
        if ("CUSTOM".equals(dataScopeType)) {
            long scopeId = idGenerator.nextId();
            mapper.insertRoleScope(idGenerator.nextId(), roleId, dataScopeType, scopeId);
            for (long departmentId : parseIdSet(departmentIdValues, "Invalid department ID")) {
                mapper.insertScopeDepartment(idGenerator.nextId(), scopeId, departmentId);
            }
            return;
        }
        mapper.insertRoleScope(idGenerator.nextId(), roleId, dataScopeType, null);
    }

    private SystemRoleDetailVO toRoleDetail(SystemRole role) {
        List<String> menuIds = mapper.findMenuIdsByRoleId(role.id()).stream().map(String::valueOf).toList();
        List<RoleDataScope> scopes = mapper.findRoleScopesByRoleId(role.id());
        RoleDataScope scope = scopes.isEmpty() ? new RoleDataScope("SELF", null) : scopes.get(0);
        List<String> departmentIds = scope.scopeId() == null
                ? List.of()
                : mapper.findDepartmentIdsByScopeId(scope.scopeId()).stream().map(String::valueOf).toList();
        return SystemRoleDetailVO.from(role, scope.scopeType(), menuIds, departmentIds);
    }

    private SystemRole requireRole(long roleId) {
        SystemRole role = mapper.findRoleById(roleId);
        if (role == null) {
            throw new ResourceNotFoundException("Role not found");
        }
        return role;
    }

    private void validateRolePayload(String code, String status, String dataScopeType, List<String> menuIds, List<String> departmentIds) {
        if (!ROLE_CODE_PATTERN.matcher(code).matches()) {
            throw new OrganizationReferenceInvalidException("Role code must use uppercase letters, numbers or underscore");
        }
        if (!ROLE_STATUSES.contains(status)) {
            throw new OrganizationReferenceInvalidException("Invalid role status");
        }
        String normalizedScopeType = normalizeDataScopeType(dataScopeType);
        Set<Long> parsedMenuIds = parseIdSet(menuIds, "Invalid menu ID");
        for (long menuId : parsedMenuIds) {
            if (mapper.countActiveMenuById(menuId) != 1) {
                throw new OrganizationReferenceInvalidException("Menu is missing or inactive");
            }
        }
        Set<Long> parsedDepartmentIds = parseIdSet(departmentIds, "Invalid department ID");
        if ("CUSTOM".equals(normalizedScopeType)) {
            if (parsedDepartmentIds.isEmpty()) {
                throw new OrganizationReferenceInvalidException("Custom data scope requires at least one department");
            }
            for (long departmentId : parsedDepartmentIds) {
                if (mapper.countActiveDepartmentById(departmentId) != 1) {
                    throw new OrganizationReferenceInvalidException("Department is missing or inactive");
                }
            }
            return;
        }
        if (!parsedDepartmentIds.isEmpty()) {
            throw new OrganizationReferenceInvalidException("Only custom data scope can bind departments");
        }
    }

    private List<String> roleIds(long userId) {
        return mapper.findRoleIdsByUserId(userId).stream().map(roleId -> Long.toString(roleId)).toList();
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new OrganizationReferenceInvalidException("Invalid pagination");
        }
    }

    private int parseVersion(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new OrganizationReferenceInvalidException("Invalid version");
        }
    }

    private Set<Long> parseRoleIds(List<String> values) {
        return parseIdSet(values, "Invalid role ID");
    }

    private Set<Long> parseIdSet(List<String> values, String message) {
        try {
            return values.stream()
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Long::parseLong)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        } catch (NumberFormatException exception) {
            throw new OrganizationReferenceInvalidException(message);
        }
    }

    private String normalizeRoleCode(String value) {
        String code = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            throw new OrganizationReferenceInvalidException("Role code is required");
        }
        return code;
    }

    private String normalizeDataScopeType(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!DATA_SCOPE_TYPES.contains(normalized)) {
            throw new OrganizationReferenceInvalidException("Invalid data scope type");
        }
        return normalized;
    }
}