package com.hrpm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PerformanceConfigurationDTOs {
    private PerformanceConfigurationDTOs() { }
    public record Metric(@NotBlank String code, @NotBlank String name, @NotBlank String metricType, String unit,
                         @NotBlank String scoreMethod, @NotBlank String scoreConfig, String description, @NotBlank String status) { }
    public record MetricUpdate(@NotBlank String name, @NotBlank String metricType, String unit, @NotBlank String scoreMethod,
                               @NotBlank String scoreConfig, String description, @NotBlank String status, @Min(0) int version) { }
    public record Scheme(@NotBlank String code, @NotBlank String name, @NotBlank String applicabilityRule) { }
    public record Version(@NotBlank String evaluationStages, @NotEmpty List<@Valid Item> items,
                          @NotEmpty List<@Valid Rule> levelRules) { }
    public record Item(@NotNull Long metricId, @NotNull @DecimalMin("0.01") BigDecimal weight,
                       @NotBlank String scoreMethod, @NotBlank String scoreConfig, @Min(0) int sortNo) { }
    public record Rule(@NotBlank String levelCode, @NotNull BigDecimal minScore, @NotNull BigDecimal maxScore,
                       boolean includeMin, boolean includeMax) { }
    public record VersionUpdate(@NotBlank String evaluationStages, @NotEmpty List<@Valid Item> items,
                                @NotEmpty List<@Valid Rule> levelRules, @Min(0) int version) { }
    public record VersionAction(@Min(0) int version) { }
    public record Cycle(@NotBlank String code, @NotBlank String name, @NotNull Long schemeVersionId,
                        @NotNull LocalDate startDate, @NotNull LocalDate endDate, @NotNull LocalDateTime selfDeadline,
                        @NotNull LocalDateTime managerDeadline, LocalDateTime appealDeadline, @NotBlank String applicabilityRule) { }
    public record CycleAction(@Min(0) int version) { }
}
