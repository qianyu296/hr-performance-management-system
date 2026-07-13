package com.hrpm.controller;

import com.hrpm.common.ApiResponse;
import com.hrpm.dto.CreateWorkflowTemplateDTO;
import com.hrpm.dto.UpdateWorkflowTemplateDTO;
import com.hrpm.service.WorkflowTemplateService;
import com.hrpm.vo.WorkflowTemplateVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflow/templates")
@PreAuthorize("hasAuthority('workflow:manage')")
public class WorkflowTemplateController {
    private final WorkflowTemplateService service;

    public WorkflowTemplateController(WorkflowTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<WorkflowTemplateVO>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkflowTemplateVO> get(@PathVariable long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    public ApiResponse<WorkflowTemplateVO> create(@Valid @RequestBody CreateWorkflowTemplateDTO request) {
        return ApiResponse.success(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<WorkflowTemplateVO> update(@PathVariable long id, @Valid @RequestBody UpdateWorkflowTemplateDTO request) {
        return ApiResponse.success(service.update(id, request));
    }
}
