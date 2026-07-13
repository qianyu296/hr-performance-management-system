package com.hrpm.controller;


import com.hrpm.common.ApiResponse;
import com.hrpm.mapper.LeaveTypeMapper;
import com.hrpm.vo.LeaveTypeVO;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leave-types")
public class LeaveTypeController {
    private final LeaveTypeMapper leaveTypeMapper;

    public LeaveTypeController(LeaveTypeMapper leaveTypeMapper) {
        this.leaveTypeMapper = leaveTypeMapper;
    }

    @GetMapping
    public ApiResponse<List<LeaveTypeVO>> listActive() {
        return ApiResponse.success(leaveTypeMapper.listActive().stream().map(LeaveTypeVO::from).toList());
    }
}
