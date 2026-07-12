package com.hrpm.service;


import com.hrpm.common.exception.InvalidPerformanceSchemeException;
import com.hrpm.entity.LevelRule;
import com.hrpm.entity.PerformanceScore;
import com.hrpm.entity.ScoreItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PerformanceScoringService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    public PerformanceScore score(List<ScoreItem> items, List<LevelRule> levelRules) {
        if (items == null || items.isEmpty()) {
            throw new InvalidPerformanceSchemeException("Performance scheme requires score items");
        }
        BigDecimal totalWeight = items.stream()
                .map(ScoreItem::weightPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(ONE_HUNDRED) != 0) {
            throw new InvalidPerformanceSchemeException("Performance scheme weights must total 100");
        }
        BigDecimal totalScore = items.stream()
                .map(item -> item.rawScore().multiply(item.weightPercent()).divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        List<LevelRule> matchingRules = levelRules.stream().filter(rule -> rule.contains(totalScore)).toList();
        if (matchingRules.size() != 1) {
            throw new InvalidPerformanceSchemeException("Performance level rules must map score exactly once");
        }
        return new PerformanceScore(totalScore, matchingRules.get(0).code());
    }
}
