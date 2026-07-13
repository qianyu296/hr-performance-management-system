package com.hrpm.controller;

import com.hrpm.common.ApiResponse;
import com.hrpm.dto.CreateOvertimeRequestDTO;
import com.hrpm.dto.SubmitLeaveRequestDTO;
import com.hrpm.security.AuthenticatedUser;
import com.hrpm.service.OvertimeRequestService;
import com.hrpm.vo.OvertimeRequestListVO;
import com.hrpm.vo.OvertimeRequestVO;
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
@RequestMapping("/overtime-requests")
public class OvertimeRequestController {
    private final OvertimeRequestService service;

    public OvertimeRequestController(OvertimeRequestService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAuthority('attendance:submit')")
    public ApiResponse<OvertimeRequestVO> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @Valid @RequestBody CreateOvertimeRequestDTO request) {
        return ApiResponse.success(OvertimeRequestVO.from(service.createDraft(user.userId(), request)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('attendance:submit')")
    public ApiResponse<List<OvertimeRequestListVO>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(service.listForUser(user.userId()));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('attendance:submit')")
    public ApiResponse<OvertimeRequestVO> submit(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
                                                  @Valid @RequestBody SubmitLeaveRequestDTO request) {
        return ApiResponse.success(OvertimeRequestVO.from(service.submit(user.userId(), id, request.version())));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('attendance:submit')")
    public ApiResponse<OvertimeRequestVO> cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
                                                  @Valid @RequestBody SubmitLeaveRequestDTO request) {
        return ApiResponse.success(OvertimeRequestVO.from(service.cancel(user.userId(), id, request.version())));
    }
}
