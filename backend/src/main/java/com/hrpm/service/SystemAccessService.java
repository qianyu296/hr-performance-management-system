package com.hrpm.service;

import com.hrpm.common.IdGenerator;
import com.hrpm.common.exception.OrganizationReferenceInvalidException;
import com.hrpm.common.exception.ResourceNotFoundException;
import com.hrpm.common.exception.VersionConflictException;
import com.hrpm.dto.UpdateUserRolesDTO;
import com.hrpm.entity.SystemRole;
import com.hrpm.entity.SystemUser;
import com.hrpm.mapper.SystemAccessMapper;
import com.hrpm.vo.PageVO;
import com.hrpm.vo.SystemRoleVO;
import com.hrpm.vo.SystemUserVO;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemAccessService {
    private final SystemAccessMapper mapper;
    private final IdGenerator idGenerator;

    public SystemAccessService(SystemAccessMapper mapper, IdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    public List<SystemRoleVO> listRoles() {
        return mapper.findRoles().stream().map(SystemRoleVO::from).toList();
    }

    public PageVO<SystemUserVO> listUsers(int page, int pageSize) {
        validatePage(page, pageSize);
        List<SystemUserVO> records = mapper.findUsers((page - 1) * pageSize, pageSize).stream()
                .map(user -> SystemUserVO.from(user, roleIds(user.id())))
                .toList();
        return new PageVO<>(records, mapper.countUsers(), page, pageSize);
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
        try {
            return values.stream().map(Long::parseLong).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        } catch (NumberFormatException exception) {
            throw new OrganizationReferenceInvalidException("Invalid role ID");
        }
    }
}
