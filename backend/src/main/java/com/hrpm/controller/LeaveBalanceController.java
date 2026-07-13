package com.hrpm.controller;

import com.hrpm.common.ApiResponse;
import com.hrpm.dto.AdjustLeaveBalanceDTO;
import com.hrpm.security.AuthenticatedUser;
import com.hrpm.service.LeaveBalanceService;
import com.hrpm.vo.LeaveBalanceChangeVO;
import com.hrpm.vo.LeaveBalanceVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leave-balances")
public class LeaveBalanceController {
    private final LeaveBalanceService service;

    public LeaveBalanceController(LeaveBalanceService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('attendance:submit', 'attendance:balance:adjust')")
    public ApiResponse<List<LeaveBalanceVO>> listMine(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(service.listMine(user.userId()));
    }

    @GetMapping("/employees/{employeeId}")
    @PreAuthorize("hasAuthority('attendance:balance:adjust')")
    public ApiResponse<List<LeaveBalanceVO>> listForEmployee(@PathVariable long employeeId) {
        return ApiResponse.success(service.listByEmployee(employeeId));
    }

    @GetMapping("/{id}/changes")
    @PreAuthorize("hasAuthority('attendance:balance:adjust')")
    public ApiResponse<List<LeaveBalanceChangeVO>> changes(@PathVariable long id) {
        return ApiResponse.success(service.listChanges(id));
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasAuthority('attendance:balance:adjust')")
    public ApiResponse<LeaveBalanceVO> adjust(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
            @Valid @RequestBody AdjustLeaveBalanceDTO request) {
        return ApiResponse.success(service.adjust(user.userId(), id, request));
    }
}
