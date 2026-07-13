package com.hrpm.controller;


import com.hrpm.common.ApiResponse;
import com.hrpm.dto.CreateLeaveTypeDTO;
import com.hrpm.dto.DisableLeaveTypeDTO;
import com.hrpm.dto.UpdateLeaveTypeDTO;
import com.hrpm.mapper.LeaveTypeMapper;
import com.hrpm.service.LeaveTypeService;
import com.hrpm.vo.LeaveTypeVO;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leave-types")
public class LeaveTypeController {
    private final LeaveTypeMapper leaveTypeMapper;
    private final LeaveTypeService leaveTypeService;

    public LeaveTypeController(LeaveTypeMapper leaveTypeMapper, LeaveTypeService leaveTypeService) {
        this.leaveTypeMapper = leaveTypeMapper;
        this.leaveTypeService = leaveTypeService;
    }

    @GetMapping
    @PreAuthorize("!#includeInactive || hasAuthority('attendance:manage')")
    public ApiResponse<List<LeaveTypeVO>> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return ApiResponse.success(includeInactive ? leaveTypeService.listAll()
                : leaveTypeMapper.listActive().stream().map(LeaveTypeVO::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('attendance:manage')")
    public ApiResponse<LeaveTypeVO> create(@Valid @RequestBody CreateLeaveTypeDTO request) {
        return ApiResponse.success(leaveTypeService.create(request));
    }

    @PatchMapping("/{id}") @PreAuthorize("hasAuthority('attendance:manage')")
    public ApiResponse<LeaveTypeVO> update(@PathVariable long id, @Valid @RequestBody UpdateLeaveTypeDTO request) { return ApiResponse.success(leaveTypeService.update(id, request)); }
    @PostMapping("/{id}/disable") @PreAuthorize("hasAuthority('attendance:manage')")
    public ApiResponse<LeaveTypeVO> disable(@PathVariable long id, @Valid @RequestBody DisableLeaveTypeDTO request) { return ApiResponse.success(leaveTypeService.disable(id, request)); }
}
