package com.hrpm.security;

import java.util.Collection;

@FunctionalInterface
public interface PermissionResolver {
    Collection<String> permissionsFor(long userId);
}
