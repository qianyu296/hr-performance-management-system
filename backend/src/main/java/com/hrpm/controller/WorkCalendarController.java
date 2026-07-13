package com.hrpm.controller;

import com.hrpm.common.ApiResponse;
import com.hrpm.dto.CreateWorkCalendarDTO;
import com.hrpm.dto.UpdateWorkCalendarDTO;
import com.hrpm.service.WorkCalendarService;
import com.hrpm.vo.WorkCalendarVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/work-calendars")
@PreAuthorize("hasAuthority('attendance:manage')")
public class WorkCalendarController {
    private final WorkCalendarService service;

    public WorkCalendarController(WorkCalendarService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<WorkCalendarVO> getByYear(@RequestParam int year) {
        return ApiResponse.success(service.getByYear(year));
    }

    @PostMapping
    public ApiResponse<WorkCalendarVO> create(@Valid @RequestBody CreateWorkCalendarDTO request) {
        return ApiResponse.success(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<WorkCalendarVO> update(@PathVariable long id, @Valid @RequestBody UpdateWorkCalendarDTO request) {
        return ApiResponse.success(service.update(id, request));
    }
}
