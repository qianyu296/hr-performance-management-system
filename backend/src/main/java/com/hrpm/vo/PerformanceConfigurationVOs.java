package com.hrpm.vo;

import com.hrpm.entity.PerformanceMetric;
import com.hrpm.entity.PerformanceConfigurationModels.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PerformanceConfigurationVOs {
    private PerformanceConfigurationVOs() { }
    public record MetricVO(String id, String code, String name, String metricType, String unit, String scoreMethod,
                           String scoreConfig, String description, String status, int version) {
        public static MetricVO from(PerformanceMetric value) { return new MetricVO(Long.toString(value.id()), value.code(), value.name(), value.metricType(), value.unit(), value.scoreMethod(), value.scoreConfig(), value.description(), value.status(), value.version()); }
    }
    public record ItemVO(String id, String metricId, BigDecimal weight, String scoreMethod, String scoreConfig, int sortNo) {
        public static ItemVO from(SchemeItem value) { return new ItemVO(Long.toString(value.id()), Long.toString(value.metricId()), value.weight(), value.scoreMethod(), value.scoreConfig(), value.sortNo()); }
    }
    public record RuleVO(String id, String levelCode, BigDecimal minScore, BigDecimal maxScore, boolean includeMin, boolean includeMax) {
        public static RuleVO from(LevelRuleRow value) { return new RuleVO(Long.toString(value.id()), value.levelCode(), value.minScore(), value.maxScore(), value.includeMin(), value.includeMax()); }
    }
    public record VersionVO(String id, String schemeId, int versionNo, String evaluationStages, String status, int version, List<ItemVO> items, List<RuleVO> levelRules) { }
    public record SchemeVO(String id, String code, String name, String applicabilityRule, String status, int version, List<VersionVO> versions) { }
    public record CycleVO(String id, String code, String name, String schemeVersionId, LocalDate startDate, LocalDate endDate,
                          LocalDateTime selfDeadline, LocalDateTime managerDeadline, LocalDateTime appealDeadline,
                          String applicabilityRule, String status, int version) {
        public static CycleVO from(Cycle value) { return new CycleVO(Long.toString(value.id()), value.code(), value.name(), Long.toString(value.schemeVersionId()), value.startDate(), value.endDate(), value.selfDeadline(), value.managerDeadline(), value.appealDeadline(), value.applicabilityRule(), value.status(), value.version()); }
    }
}
