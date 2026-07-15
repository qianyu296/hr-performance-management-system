package com.hrpm.service;

import com.hrpm.entity.WorkflowBusinessContext;
import org.springframework.stereotype.Service;

@Service
public class PersonnelChangeWorkflowBusinessHandler implements WorkflowBusinessHandler {
    private final PersonnelChangeService personnelChangeService;

    public PersonnelChangeWorkflowBusinessHandler(PersonnelChangeService personnelChangeService) {
        this.personnelChangeService = personnelChangeService;
    }

    @Override
    public String businessType() {
        return "PERSONNEL_CHANGE";
    }

    @Override
    public void approve(WorkflowBusinessContext context) {
        personnelChangeService.markApproved(context.businessId(), context.actorUserId());
    }

    @Override
    public void reject(WorkflowBusinessContext context) {
        personnelChangeService.markRejected(context.businessId(), context.actorUserId());
    }

    @Override
    public void withdraw(WorkflowBusinessContext context) {
        personnelChangeService.markWithdrawn(context.businessId(), context.actorUserId());
    }

    @Override
    public void returnToDraft(WorkflowBusinessContext context) {
        personnelChangeService.returnToDraft(context.businessId(), context.actorUserId());
    }
}
