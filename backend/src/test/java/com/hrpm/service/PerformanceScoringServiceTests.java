package com.hrpm.service;


import com.hrpm.common.exception.InvalidPerformanceSchemeException;
import com.hrpm.entity.LevelRule;
import com.hrpm.entity.PerformanceScore;
import com.hrpm.entity.ScoreItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PerformanceScoringServiceTests {
    private final PerformanceScoringService service = new PerformanceScoringService();

    @Test
    void calculatesWeightedScoreAndMapsConfiguredBoundaryToHigherLevel() {
        List<ScoreItem> items = List.of(
                new ScoreItem("delivery", new BigDecimal("60.00"), new BigDecimal("95.00")),
                new ScoreItem("collaboration", new BigDecimal("40.00"), new BigDecimal("82.50")));
        List<LevelRule> levels = List.of(
                new LevelRule("A", new BigDecimal("90.00"), new BigDecimal("100.00"), true, true),
                new LevelRule("B", new BigDecimal("80.00"), new BigDecimal("90.00"), true, false),
                new LevelRule("C", new BigDecimal("0.00"), new BigDecimal("80.00"), true, false));

        PerformanceScore result = service.score(items, levels);

        assertEquals(new BigDecimal("90.00"), result.totalScore());
        assertEquals("A", result.levelCode());
    }

    @Test
    void preservesTwoDecimalPrecisionBeforeLevelMapping() {
        List<ScoreItem> items = List.of(
                new ScoreItem("delivery", new BigDecimal("50.00"), new BigDecimal("89.99")),
                new ScoreItem("collaboration", new BigDecimal("50.00"), new BigDecimal("89.99")));
        List<LevelRule> levels = List.of(
                new LevelRule("A", new BigDecimal("90.00"), new BigDecimal("100.00"), true, true),
                new LevelRule("B", new BigDecimal("80.00"), new BigDecimal("90.00"), true, false));

        PerformanceScore result = service.score(items, levels);

        assertEquals(new BigDecimal("89.99"), result.totalScore());
        assertEquals("B", result.levelCode());
    }

    @Test
    void rejectsSchemeWhoseWeightsDoNotTotalOneHundredPercent() {
        List<ScoreItem> items = List.of(
                new ScoreItem("delivery", new BigDecimal("50.00"), new BigDecimal("95.00")),
                new ScoreItem("collaboration", new BigDecimal("40.00"), new BigDecimal("82.50")));

        assertThrows(InvalidPerformanceSchemeException.class, () -> service.score(items, List.of()));
    }
}
