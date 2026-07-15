package com.hrpm.controller;

import com.hrpm.common.ApiResponse;
import com.hrpm.dto.CreateEmployeeDTO;
import com.hrpm.dto.UpdateEmployeeDTO;
import com.hrpm.security.AuthenticatedUser;
import com.hrpm.service.EmployeeService;
import com.hrpm.service.PersonnelChangeService;
import com.hrpm.vo.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {
    private final EmployeeService service;
    private final PersonnelChangeService personnelChangeService;

    public EmployeeController(EmployeeService service, PersonnelChangeService personnelChangeService) {
        this.service = service;
        this.personnelChangeService = personnelChangeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('org:read')")
    public ApiResponse<PageVO<EmployeeListVO>> list(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam(defaultValue="1") int page,
                                                     @RequestParam(defaultValue="20") int pageSize,
                                                     @RequestParam(required=false) String keyword,
                                                     @RequestParam(required=false) Long departmentId,
                                                     @RequestParam(required=false) Long positionId,
                                                     @RequestParam(required=false) String employmentStatus) {
        return ApiResponse.success(service.list(user.userId(), page, pageSize, keyword, departmentId, positionId, employmentStatus));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('org:read')")
    public ApiResponse<EmployeeVO> get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id) {
        return ApiResponse.success(EmployeeVO.from(service.getForUser(user.userId(), id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('org:manage')")
    public ApiResponse<CreatedEmployeeVO> create(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateEmployeeDTO request) {
        return ApiResponse.success(service.create(user.userId(), request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('org:manage')")
    public ApiResponse<EmployeeVO> update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
                                          @Valid @RequestBody UpdateEmployeeDTO request) {
        return ApiResponse.success(EmployeeVO.from(service.update(user.userId(), id, request)));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('org:read')")
    public ApiResponse<List<com.hrpm.vo.PersonnelChangeVOs.EmployeeHistoryVO>> history(@AuthenticationPrincipal AuthenticatedUser user,
                                                                                        @PathVariable long id) {
        return ApiResponse.success(personnelChangeService.history(user.userId(), id));
    }
}
