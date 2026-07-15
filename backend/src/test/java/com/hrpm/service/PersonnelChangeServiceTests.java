package com.hrpm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpm.common.IdGenerator;
import com.hrpm.common.exception.DataScopeDeniedException;
import com.hrpm.dto.PersonnelChangeDTOs.ChangeAction;
import com.hrpm.entity.PersonnelChange;
import com.hrpm.mapper.DepartmentMapper;
import com.hrpm.mapper.EmployeeMapper;
import com.hrpm.mapper.PersonnelChangeMapper;
import com.hrpm.mapper.PositionMapper;
import com.hrpm.mapper.RankMapper;
import com.hrpm.mapper.UserAccountMapper;
import com.hrpm.mapper.UserPermissionMapper;
import com.hrpm.mapper.WorkflowMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class PersonnelChangeServiceTests {
    private final PersonnelChangeMapper personnelChangeMapper = Mockito.mock(PersonnelChangeMapper.class);
    private final PersonnelChangeService service = new PersonnelChangeService(
            personnelChangeMapper,
            Mockito.mock(WorkflowMapper.class),
            Mockito.mock(EmployeeMapper.class),
            Mockito.mock(DepartmentMapper.class),
            Mockito.mock(PositionMapper.class),
            Mockito.mock(RankMapper.class),
            Mockito.mock(UserAccountMapper.class),
            Mockito.mock(UserPermissionMapper.class),
            Mockito.mock(OrganizationAccessService.class),
            Mockito.mock(EmployeeDataScopeResolver.class),
            Mockito.mock(WorkflowApproverResolver.class),
            Mockito.mock(EmployeeService.class),
            Mockito.mock(OperationAuditService.class),
            Mockito.mock(IdGenerator.class),
            new ObjectMapper());

    @Test
    void markRejectedRequiresInProgressChange() {
        when(personnelChangeMapper.findById(10L)).thenReturn(change(10L, "DRAFT", 0, 1L));

        assertThrows(IllegalStateException.class, () -> service.markRejected(10L, 1L));
    }

    @Test
    void returnToDraftRequiresInProgressChange() {
        when(personnelChangeMapper.findById(11L)).thenReturn(change(11L, "APPROVED", 2, 1L));

        assertThrows(IllegalStateException.class, () -> service.returnToDraft(11L, 1L));
    }

    @Test
    void withdrawRequiresCreatorOwnership() {
        when(personnelChangeMapper.findById(12L)).thenReturn(change(12L, "IN_PROGRESS", 1, 100L));

        assertThrows(DataScopeDeniedException.class, () -> service.withdraw(200L, 12L, new ChangeAction("1")));
    }

    private PersonnelChange change(long id, String status, int version, long createdBy) {
        return new PersonnelChange(id, "PC-" + id, 99L, "TRANSFER", LocalDate.now(), LocalDate.now(),
                "reason", "{\"name\":\"before\"}", "{\"name\":\"after\"}", 88L, status, createdBy,
                LocalDateTime.now(), version);
    }
}
