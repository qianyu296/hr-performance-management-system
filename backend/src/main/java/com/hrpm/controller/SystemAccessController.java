package com.hrpm.controller;

import com.hrpm.common.ApiResponse;
import com.hrpm.dto.UpdateUserRolesDTO;
import com.hrpm.service.SystemAccessService;
import com.hrpm.vo.PageVO;
import com.hrpm.vo.SystemRoleVO;
import com.hrpm.vo.SystemUserVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system")
@PreAuthorize("hasAuthority('system:manage')")
public class SystemAccessController {
    private final SystemAccessService service;

    public SystemAccessController(SystemAccessService service) {
        this.service = service;
    }

    @GetMapping("/roles")
    public ApiResponse<List<SystemRoleVO>> listRoles() {
        return ApiResponse.success(service.listRoles());
    }

    @GetMapping("/users")
    public ApiResponse<PageVO<SystemUserVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.listUsers(page, pageSize));
    }

    @PutMapping("/users/{id}/roles")
    public ApiResponse<SystemUserVO> updateUserRoles(
            @PathVariable long id,
            @Valid @RequestBody UpdateUserRolesDTO request) {
        return ApiResponse.success(service.updateUserRoles(id, request));
    }
}
