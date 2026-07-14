package com.hrpm.service;

import com.hrpm.entity.ReportModels.DepartmentHeadcount;
import com.hrpm.entity.ReportModels.PerformanceLevelDistribution;
import com.hrpm.mapper.ReportMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final ReportMapper reportMapper;
    public ReportService(ReportMapper reportMapper) { this.reportMapper = reportMapper; }
    public List<DepartmentHeadcount> departmentHeadcounts() { return reportMapper.departmentHeadcounts(); }
    public List<PerformanceLevelDistribution> performanceLevelDistribution() { return reportMapper.publishedPerformanceLevelDistribution(); }
}
