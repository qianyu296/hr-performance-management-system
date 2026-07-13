package com.hrpm.controller;


import com.hrpm.common.ApiResponse;
import com.hrpm.dto.CreateDepartmentDTO;
import com.hrpm.service.DepartmentService;
import com.hrpm.vo.DepartmentVO;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/departments")
public class DepartmentController {
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('org:read')")
    public ApiResponse<List<DepartmentVO>> listTree() {
        return ApiResponse.success(departmentService.listTree());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('org:manage')")
    public ApiResponse<DepartmentVO> create(@Valid @RequestBody CreateDepartmentDTO request) {
        return ApiResponse.success(DepartmentVO.from(departmentService.create(request)));
    }
}
