package com.hrpm.controller;


import com.hrpm.common.ApiResponse;
import com.hrpm.security.AuthenticatedUser;
import com.hrpm.security.PermissionResolver;
import com.hrpm.mapper.UserPermissionMapper;
import com.hrpm.vo.AuthorizedMenuVO;
import com.hrpm.vo.CurrentUserVO;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrentUserController {
    private final PermissionResolver permissionResolver;
    private final UserPermissionMapper userPermissionMapper;

    public CurrentUserController(PermissionResolver permissionResolver, UserPermissionMapper userPermissionMapper) {
        this.permissionResolver = permissionResolver;
        this.userPermissionMapper = userPermissionMapper;
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> currentUser(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(new CurrentUserVO(Long.toString(user.userId()), user.username()));
    }

    @GetMapping("/me/permissions")
    public ApiResponse<List<String>> permissions(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(List.copyOf(permissionResolver.permissionsFor(user.userId())));
    }

    @GetMapping("/me/menus")
    public ApiResponse<List<AuthorizedMenuVO>> menus(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(userPermissionMapper.findAuthorizedMenusByUserId(user.userId()).stream()
                .map(AuthorizedMenuVO::from)
                .toList());
    }
}
