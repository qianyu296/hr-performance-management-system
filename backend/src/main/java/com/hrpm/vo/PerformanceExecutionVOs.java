package com.hrpm.vo;

import java.math.BigDecimal;
import java.util.List;

public final class PerformanceExecutionVOs {
    private PerformanceExecutionVOs() { }
    public record ScoreVO(String stage, String rawScore, String weightedScore, String comment) { }
    public record TaskItemVO(String id, String metricSnapshot, BigDecimal weight, int version, List<ScoreVO> scores) { }
    public record TaskVO(String id, String cycleId, String cycleName, String employeeId, String employeeName,
                         String managerEmployeeId, String status, int version, List<TaskItemVO> items,
                         String totalScore, String levelCode, String publishStatus) { }
}
