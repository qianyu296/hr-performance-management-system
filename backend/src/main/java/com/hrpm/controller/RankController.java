package com.hrpm.controller;

import com.hrpm.common.ApiResponse;
import com.hrpm.dto.CreateRankDTO;
import com.hrpm.dto.UpdateRankDTO;
import com.hrpm.service.RankService;
import com.hrpm.vo.RankVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/ranks")
public class RankController {
    private final RankService service;
    public RankController(RankService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('org:read')")
    public ApiResponse<List<RankVO>> list() { return ApiResponse.success(service.list().stream().map(RankVO::from).toList()); }

    @PostMapping
    @PreAuthorize("hasAuthority('org:manage')")
    public ApiResponse<RankVO> create(@Valid @RequestBody CreateRankDTO request) { return ApiResponse.success(RankVO.from(service.create(request))); }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('org:manage')")
    public ApiResponse<RankVO> update(@PathVariable long id, @Valid @RequestBody UpdateRankDTO request) { return ApiResponse.success(RankVO.from(service.update(id, request))); }
}
