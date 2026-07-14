package com.hrpm.controller;


import com.hrpm.common.ApiResponse;
import com.hrpm.dto.CreateDepartmentDTO;
import com.hrpm.dto.DisableDepartmentDTO;
import com.hrpm.dto.MoveDepartmentDTO;
import com.hrpm.dto.UpdateDepartmentDTO;
import com.hrpm.security.AuthenticatedUser;
import com.hrpm.service.DepartmentService;
import com.hrpm.vo.DepartmentVO;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/departments")
public class DepartmentController {
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('org:read')")
    public ApiResponse<List<DepartmentVO>> listTree(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(departmentService.listTree(user.userId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('org:manage')")
    public ApiResponse<DepartmentVO> create(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateDepartmentDTO request) {
        return ApiResponse.success(DepartmentVO.from(departmentService.create(user.userId(), request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('org:manage')")
    public ApiResponse<DepartmentVO> update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
                                            @Valid @RequestBody UpdateDepartmentDTO request) {
        return ApiResponse.success(DepartmentVO.from(departmentService.update(user.userId(), id, request)));
    }

    @PostMapping("/{id}/move")
    @PreAuthorize("hasAuthority('org:manage')")
    public ApiResponse<DepartmentVO> move(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
                                          @Valid @RequestBody MoveDepartmentDTO request) {
        return ApiResponse.success(DepartmentVO.from(departmentService.move(user.userId(), id, request)));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('org:manage')")
    public ApiResponse<DepartmentVO> disable(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
                                             @Valid @RequestBody DisableDepartmentDTO request) {
        return ApiResponse.success(DepartmentVO.from(departmentService.disable(user.userId(), id, request)));
    }
}
