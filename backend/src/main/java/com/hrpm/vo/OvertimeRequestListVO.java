package com.hrpm.vo;

import java.math.BigDecimal;
import java.time.Instant;

public record OvertimeRequestListVO(String id, String requestNo, Instant startTime, Instant endTime,
                                    BigDecimal durationHours, String compensationType, String status,
                                    String workflowInstanceId, int version) {
}
