package com.hrpm.entity;

import java.time.LocalDateTime;

public record ExitHandoverItem(long id, long handoverId, String itemType, Long receiverEmployeeId,
                               boolean required, String status, LocalDateTime completedTime,
                               Long confirmedBy, String remark, int version) {
}
