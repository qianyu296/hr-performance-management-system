package com.hrpm.service;


import com.hrpm.mapper.UserPermissionMapper;
import com.hrpm.security.PermissionResolver;

import java.util.Collection;
import org.springframework.stereotype.Component;

@Component
public class DatabasePermissionResolver implements PermissionResolver {
    private final UserPermissionMapper userPermissionMapper;

    public DatabasePermissionResolver(UserPermissionMapper userPermissionMapper) {
        this.userPermissionMapper = userPermissionMapper;
    }

    @Override
    public Collection<String> permissionsFor(long userId) {
        return userPermissionMapper.findPermissionCodesByUserId(userId);
    }
}
