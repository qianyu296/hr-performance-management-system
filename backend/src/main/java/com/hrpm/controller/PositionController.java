package com.hrpm.controller;

import com.hrpm.common.ApiResponse;
import com.hrpm.dto.CreatePositionDTO;
import com.hrpm.dto.UpdatePositionDTO;
import com.hrpm.service.PositionService;
import com.hrpm.vo.PositionVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/positions")
public class PositionController {
    private final PositionService service;
    public PositionController(PositionService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('org:read')")
    public ApiResponse<List<PositionVO>> list() { return ApiResponse.success(service.list().stream().map(PositionVO::from).toList()); }

    @PostMapping
    @PreAuthorize("hasAuthority('org:manage')")
    public ApiResponse<PositionVO> create(@Valid @RequestBody CreatePositionDTO request) { return ApiResponse.success(PositionVO.from(service.create(request))); }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('org:manage')")
    public ApiResponse<PositionVO> update(@PathVariable long id, @Valid @RequestBody UpdatePositionDTO request) { return ApiResponse.success(PositionVO.from(service.update(id, request))); }
}
