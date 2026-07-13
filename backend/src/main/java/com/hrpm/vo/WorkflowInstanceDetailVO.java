package com.hrpm.vo;

import com.hrpm.entity.WorkflowInstance;
import java.util.List;

public record WorkflowInstanceDetailVO(String id, String businessType, String businessId, String initiatorUserId,
                                       String status, Integer currentNodeNo, String version,
                                       List<WorkflowActionLogVO> history) {
    public static WorkflowInstanceDetailVO from(WorkflowInstance instance, List<WorkflowActionLogVO> history) {
        return new WorkflowInstanceDetailVO(Long.toString(instance.id()), instance.businessType(), Long.toString(instance.businessId()),
                Long.toString(instance.initiatorUserId()), instance.status(), instance.currentNodeNo(),
                Integer.toString(instance.version()), history);
    }
}
