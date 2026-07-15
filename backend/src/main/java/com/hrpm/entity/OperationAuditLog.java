package com.hrpm.entity;

import java.time.LocalDateTime;

public record OperationAuditLog(long id, Long actorUserId, String module, String objectType,
                                Long objectId, String action, String result, String traceId,
                                String summary, String sourceAddress, LocalDateTime createdTime) {
}
