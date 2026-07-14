package com.hrpm.entity;

public record PerformanceMetric(long id, String code, String name, String metricType, String unit,
                                String scoreMethod, String scoreConfig, String description, String status, int version) {
}
