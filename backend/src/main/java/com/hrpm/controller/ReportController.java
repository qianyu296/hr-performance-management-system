package com.hrpm.controller;

import com.hrpm.common.ApiResponse;
import com.hrpm.entity.ReportModels.DepartmentHeadcount;
import com.hrpm.entity.ReportModels.PerformanceLevelDistribution;
import com.hrpm.service.ReportService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@PreAuthorize("hasAuthority('report:read')")
public class ReportController {
    private final ReportService reportService;
    public ReportController(ReportService reportService) { this.reportService = reportService; }
    @GetMapping("/headcount-by-department") public ApiResponse<List<DepartmentHeadcount>> headcountByDepartment() { return ApiResponse.success(reportService.departmentHeadcounts()); }
    @GetMapping("/performance-level-distribution") public ApiResponse<List<PerformanceLevelDistribution>> performanceLevelDistribution() { return ApiResponse.success(reportService.performanceLevelDistribution()); }
}
