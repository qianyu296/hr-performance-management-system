package com.hrpm.controller;


import com.hrpm.common.ApiResponse;
import com.hrpm.dto.WorkflowActionDTO;
import com.hrpm.security.AuthenticatedUser;
import com.hrpm.service.WorkflowTaskService;
import com.hrpm.vo.WorkflowTaskListVO;

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
}
