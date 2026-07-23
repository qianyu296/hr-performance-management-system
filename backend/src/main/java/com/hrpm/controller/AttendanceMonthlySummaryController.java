package com.hrpm.controller;

import com.hrpm.common.ApiResponse;
import com.hrpm.dto.RebuildAttendanceMonthlySummaryDTO;
import com.hrpm.security.AuthenticatedUser;
import com.hrpm.service.AttendanceMonthlySummaryService;
import com.hrpm.vo.AttendanceMonthlySummaryVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/attendance/monthly-summaries")
public class AttendanceMonthlySummaryController {
    private final AttendanceMonthlySummaryService service;

    public AttendanceMonthlySummaryController(AttendanceMonthlySummaryService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('report:read', 'attendance:manage')")
    public ApiResponse<List<AttendanceMonthlySummaryVO>> list(@RequestParam String month,
                                                               @RequestParam(required = false) Long departmentId,
                                                               @RequestParam(required = false) Long employeeId) {
        return ApiResponse.success(service.list(month, departmentId, employeeId));
    }

    @PostMapping("/rebuild")
    @PreAuthorize("hasAuthority('attendance:manage')")
    public ApiResponse<java.util.Map<String, Integer>> rebuild(@AuthenticationPrincipal AuthenticatedUser user,
                                                                 @Valid @RequestBody RebuildAttendanceMonthlySummaryDTO request) {
        return ApiResponse.success(java.util.Map.of("affectedRows", service.rebuild(user.userId(), request)));
    }
}
