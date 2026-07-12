package com.hrpm.controller;


import com.hrpm.common.ApiResponse;
import com.hrpm.dto.CreateLeaveRequestDTO;
import com.hrpm.dto.SubmitLeaveRequestDTO;
import com.hrpm.security.AuthenticatedUser;
import com.hrpm.service.LeaveRequestService;
import com.hrpm.vo.LeaveRequestListVO;
import com.hrpm.vo.LeaveRequestVO;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leave-requests")
public class LeaveRequestController {
    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('attendance:submit')")
    public ApiResponse<LeaveRequestVO> createDraft(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateLeaveRequestDTO command) {
        return ApiResponse.success(LeaveRequestVO.from(leaveRequestService.createDraft(user.userId(), command)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('attendance:submit')")
    public ApiResponse<List<LeaveRequestListVO>> listMine(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(leaveRequestService.listForUser(user.userId()));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('attendance:submit')")
    public ApiResponse<LeaveRequestVO> submit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long id,
            @Valid @RequestBody SubmitLeaveRequestDTO command) {
        return ApiResponse.success(LeaveRequestVO.from(leaveRequestService.submit(user.userId(), id, command.version())));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('attendance:submit')")
    public ApiResponse<LeaveRequestVO> cancel(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long id,
            @Valid @RequestBody SubmitLeaveRequestDTO command) {
        return ApiResponse.success(LeaveRequestVO.from(leaveRequestService.cancel(user.userId(), id, command.version())));
    }
}
