package com.hrpm.controller;

import com.hrpm.common.ApiResponse;
import com.hrpm.dto.PerformanceConfigurationDTOs.*;
import com.hrpm.service.PerformanceConfigurationService;
import com.hrpm.vo.PerformanceConfigurationVOs.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/performance")
@PreAuthorize("hasAuthority('performance:config')")
public class PerformanceConfigurationController {
    private final PerformanceConfigurationService service;
    public PerformanceConfigurationController(PerformanceConfigurationService service) { this.service = service; }
    @GetMapping("/metrics") public ApiResponse<List<MetricVO>> metrics() { return ApiResponse.success(service.listMetrics()); }
    @PostMapping("/metrics") public ApiResponse<MetricVO> createMetric(@Valid @RequestBody Metric request) { return ApiResponse.success(service.createMetric(request)); }
    @PatchMapping("/metrics/{id}") public ApiResponse<MetricVO> updateMetric(@PathVariable long id, @Valid @RequestBody MetricUpdate request) { return ApiResponse.success(service.updateMetric(id, request)); }
    @GetMapping("/schemes") public ApiResponse<List<SchemeVO>> schemes() { return ApiResponse.success(service.listSchemes()); }
    @PostMapping("/schemes") public ApiResponse<SchemeVO> createScheme(@Valid @RequestBody Scheme request) { return ApiResponse.success(service.createScheme(request)); }
    @PostMapping("/schemes/{id}/versions") public ApiResponse<VersionVO> createVersion(@PathVariable long id, @Valid @RequestBody Version request) { return ApiResponse.success(service.createVersion(id, request)); }
    @PatchMapping("/scheme-versions/{id}") public ApiResponse<VersionVO> updateVersion(@PathVariable long id, @Valid @RequestBody VersionUpdate request) { return ApiResponse.success(service.updateVersion(id, request)); }
    @PostMapping("/scheme-versions/{id}/enable") public ApiResponse<VersionVO> enableVersion(@PathVariable long id, @Valid @RequestBody VersionAction request) { return ApiResponse.success(service.enableVersion(id, request)); }
    @GetMapping("/cycles") public ApiResponse<List<CycleVO>> cycles() { return ApiResponse.success(service.listCycles()); }
    @PostMapping("/cycles") public ApiResponse<CycleVO> createCycle(@Valid @RequestBody Cycle request) { return ApiResponse.success(service.createCycle(request)); }
    @PostMapping("/cycles/{id}/start") public ApiResponse<CycleVO> startCycle(@PathVariable long id, @Valid @RequestBody CycleAction request) { return ApiResponse.success(service.startCycle(id, request)); }
}
