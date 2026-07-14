package com.hrpm.controller;

import com.hrpm.common.ApiResponse;
import com.hrpm.dto.PerformanceExecutionDTOs.PublishCycle;
import com.hrpm.dto.PerformanceExecutionDTOs.SubmitScores;
import com.hrpm.security.AuthenticatedUser;
import com.hrpm.service.PerformanceExecutionService;
import com.hrpm.vo.PerformanceExecutionVOs.TaskVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/performance")
public class PerformanceExecutionController {
    private final PerformanceExecutionService service;
    public PerformanceExecutionController(PerformanceExecutionService service) { this.service = service; }
    @GetMapping("/tasks/mine") public ApiResponse<List<TaskVO>> mine(@AuthenticationPrincipal AuthenticatedUser user) { return ApiResponse.success(service.listMyTasks(user.userId())); }
    @GetMapping("/tasks/manager") public ApiResponse<List<TaskVO>> managerTasks(@AuthenticationPrincipal AuthenticatedUser user) { return ApiResponse.success(service.listManagerTasks(user.userId())); }
    @PostMapping("/tasks/{id}/self-assessment") public ApiResponse<TaskVO> selfAssessment(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id, @Valid @RequestBody SubmitScores request) { return ApiResponse.success(service.submitSelfAssessment(user.userId(), id, request)); }
    @PostMapping("/tasks/{id}/manager-score") public ApiResponse<TaskVO> managerScore(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id, @Valid @RequestBody SubmitScores request) { return ApiResponse.success(service.submitManagerScore(user.userId(), id, request)); }
    @PostMapping("/cycles/{id}/publish") @PreAuthorize("hasAuthority('performance:config')") public ApiResponse<Map<String, Integer>> publish(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id, @Valid @RequestBody PublishCycle request) { return ApiResponse.success(Map.of("publishedCount", service.publishCycle(user.userId(), id, request.version()))); }
}
