package com.hrpm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public final class PerformanceExecutionDTOs {
    private PerformanceExecutionDTOs() { }
    public record ScoreItem(@NotNull Long taskItemId, @NotNull @DecimalMin("0") BigDecimal rawScore, String comment) { }
    public record SubmitScores(@Min(0) int version, @NotEmpty List<@Valid ScoreItem> items) { }
    public record PublishCycle(@Min(0) int version) { }
}
