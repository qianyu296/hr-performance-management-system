package com.hrpm.entity;

import java.math.BigDecimal;

public record ScoreItem(String code, BigDecimal weightPercent, BigDecimal rawScore) {
    public ScoreItem {
        if (code == null || code.isBlank() || weightPercent == null || rawScore == null
                || weightPercent.signum() < 0 || rawScore.signum() < 0) {
            throw new IllegalArgumentException("Invalid score item");
        }
    }
}
