package com.hrpm.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.hrpm.entity.EmployeeHistory;
import com.hrpm.entity.ExitHandoverItem;
import com.hrpm.entity.PersonnelChange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PersonnelChangeVOs {
    private PersonnelChangeVOs() {
    }

    public record PersonnelChangeListItemVO(String id, String changeNo, String employeeId, String employeeName,
                                            String changeType, LocalDate effectiveDate, String status,
                                            String workflowInstanceId, String version) {
        public static PersonnelChangeListItemVO from(PersonnelChange value, String employeeName) {
            return new PersonnelChangeListItemVO(Long.toString(value.id()), value.changeNo(),
                    value.employeeId() == null ? null : Long.toString(value.employeeId()), employeeName,
                    value.changeType(), value.effectiveDate(), value.status(),
                    value.workflowInstanceId() == null ? null : Long.toString(value.workflowInstanceId()),
                    Integer.toString(value.version()));
        }
    }

    public record ExitHandoverItemVO(String id, String itemType, String receiverEmployeeId, boolean required,
                                     String status, LocalDateTime completedTime, String confirmedBy,
                                     String remark, String version) {
        public static ExitHandoverItemVO from(ExitHandoverItem value) {
            return new ExitHandoverItemVO(Long.toString(value.id()), value.itemType(),
                    value.receiverEmployeeId() == null ? null : Long.toString(value.receiverEmployeeId()),
                    value.required(), value.status(), value.completedTime(),
                    value.confirmedBy() == null ? null : Long.toString(value.confirmedBy()),
                    value.remark(), Integer.toString(value.version()));
        }
    }

    public record PersonnelChangeDetailVO(String id, String changeNo, String employeeId, String changeType,
                                          LocalDate applicationDate, LocalDate effectiveDate, String reason,
                                          JsonNode beforeSnapshot, JsonNode afterSnapshot, String workflowInstanceId,
                                          String status, String createdBy, LocalDateTime createdTime,
                                          String version, List<ExitHandoverItemVO> handoverItems,
                                          boolean canEdit, boolean canSubmit, boolean canWithdraw,
                                          boolean canMaintainHandover) {
    }

    public record EmployeeHistoryVO(String id, String employeeId, String changeId, String eventType,
                                    LocalDate effectiveDate, JsonNode snapshot, String createdBy,
                                    LocalDateTime createdTime) {
        public static EmployeeHistoryVO from(EmployeeHistory value, JsonNode snapshot) {
            return new EmployeeHistoryVO(Long.toString(value.id()), Long.toString(value.employeeId()),
                    value.changeId() == null ? null : Long.toString(value.changeId()),
                    value.eventType(), value.effectiveDate(), snapshot,
                    value.createdBy() == null ? null : Long.toString(value.createdBy()),
                    value.createdTime());
        }
    }
}
