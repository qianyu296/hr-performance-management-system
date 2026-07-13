package com.hrpm.entity;

import java.math.BigDecimal;

public record LevelRule(String code, BigDecimal minimum, BigDecimal maximum, boolean includesMinimum, boolean includesMaximum) {
    public LevelRule {
        if (code == null || code.isBlank() || minimum == null || maximum == null || minimum.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("Invalid performance level rule");
        }
    }

    public boolean contains(BigDecimal score) {
        int minimumComparison = score.compareTo(minimum);
        int maximumComparison = score.compareTo(maximum);
        return (includesMinimum ? minimumComparison >= 0 : minimumComparison > 0)
                && (includesMaximum ? maximumComparison <= 0 : maximumComparison < 0);
    }
}
