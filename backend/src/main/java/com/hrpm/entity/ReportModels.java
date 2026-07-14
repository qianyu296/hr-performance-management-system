package com.hrpm.entity;

public final class ReportModels {
    private ReportModels() { }
    public record DepartmentHeadcount(String departmentName, long headcount) { }
    public record PerformanceLevelDistribution(String levelCode, long count) { }
}
