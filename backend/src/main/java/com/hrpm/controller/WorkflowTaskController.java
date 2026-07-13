package com.hrpm.controller;


import com.hrpm.common.ApiResponse;
import com.hrpm.dto.WorkflowActionDTO;
import com.hrpm.dto.WorkflowInstanceActionDTO;
import com.hrpm.security.AuthenticatedUser;
import com.hrpm.service.WorkflowTaskService;
import com.hrpm.vo.WorkflowTaskListVO;
import com.hrpm.vo.WorkflowInstanceDetailVO;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflow/tasks")
public class WorkflowTaskController {
    private final WorkflowTaskService workflowTaskService;

    public WorkflowTaskController(WorkflowTaskService workflowTaskService) {
        this.workflowTaskService = workflowTaskService;
    }

    @GetMapping
    public ApiResponse<List<WorkflowTaskListVO>> listPending(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(workflowTaskService.listPending(user.userId()));
    }

    @GetMapping("/instances/{id}")
    public ApiResponse<WorkflowInstanceDetailVO> detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id) {
        return ApiResponse.success(workflowTaskService.detail(user.userId(), id));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Map<String, String>> approve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
            @Valid @RequestBody WorkflowActionDTO command) {
        return ApiResponse.success(Map.of("status", workflowTaskService.approve(user.userId(), id, command.version(), command.comment())));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Map<String, String>> reject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
            @Valid @RequestBody WorkflowActionDTO command) {
        return ApiResponse.success(Map.of("status", workflowTaskService.reject(user.userId(), id, command.version(), command.comment())));
    }

    @PostMapping("/{id}/return")
    public ApiResponse<Map<String, String>> returnToInitiator(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
            @Valid @RequestBody WorkflowActionDTO command) {
        return ApiResponse.success(Map.of("status", workflowTaskService.returnToInitiator(user.userId(), id, command.version(), command.comment())));
    }

    @PostMapping("/{id}/transfer")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('workflow:intervene')")
    public ApiResponse<Map<String, String>> transfer(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
            @Valid @RequestBody WorkflowActionDTO command) {
        if (command.transferToUserId() == null) {
            throw new com.hrpm.common.exception.WorkflowTaskInvalidException();
        }
        return ApiResponse.success(Map.of("status", workflowTaskService.transfer(user.userId(), id, command.version(), command.transferToUserId(), command.comment())));
    }

    @PostMapping("/instances/{id}/withdraw")
    public ApiResponse<Map<String, String>> withdraw(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
            @Valid @RequestBody WorkflowInstanceActionDTO command) {
        return ApiResponse.success(Map.of("status", workflowTaskService.withdraw(user.userId(), id, command.version(), command.comment())));
    }
}
