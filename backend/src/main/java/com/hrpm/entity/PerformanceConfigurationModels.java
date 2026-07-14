package com.hrpm.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class PerformanceConfigurationModels {
    private PerformanceConfigurationModels() { }
    public record Scheme(long id, String code, String name, String applicabilityRule, String status, int version) { }
    public record SchemeVersion(long id, long schemeId, int versionNo, String evaluationStages, String snapshot, String status, int version) { }
    public record SchemeItem(long id, long schemeVersionId, long metricId, BigDecimal weight, String scoreMethod, String scoreConfig, int sortNo, int version) { }
    public record LevelRuleRow(long id, long schemeVersionId, String levelCode, BigDecimal minScore, BigDecimal maxScore,
                               boolean includeMin, boolean includeMax, int version) { }
    public record Cycle(long id, String code, String name, long schemeVersionId, LocalDate startDate, LocalDate endDate,
                        LocalDateTime selfDeadline, LocalDateTime managerDeadline, LocalDateTime appealDeadline,
                        String applicabilityRule, String status, int version) { }
    public record Task(long id, long cycleId, long employeeId, Long managerEmployeeId, long schemeVersionId,
                       String organizationSnapshot, String status, int version) { }
    public record TaskItem(long id, long taskId, String metricSnapshot, BigDecimal weight, String stageSnapshot, int version) { }
    public record Score(long id, long taskItemId, String stage, long evaluatorEmployeeId, BigDecimal rawScore,
                        BigDecimal weightedScore, String comment, int version) { }
    public record Result(long id, long taskId, int currentVersionNo, String publishStatus, int version) { }
    public record ResultVersion(long id, long resultId, int versionNo, BigDecimal totalScore, String levelCode,
                                String source, String reason, LocalDateTime publishedTime) { }
}
