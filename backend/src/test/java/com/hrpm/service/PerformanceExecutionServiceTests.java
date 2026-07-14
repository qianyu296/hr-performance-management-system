package com.hrpm.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.hrpm.common.IdGenerator;
import com.hrpm.dto.PerformanceExecutionDTOs.ScoreItem;
import com.hrpm.dto.PerformanceExecutionDTOs.SubmitScores;
import com.hrpm.entity.PerformanceConfigurationModels.Task;
import com.hrpm.entity.PerformanceConfigurationModels.TaskItem;
import com.hrpm.entity.UserAccount;
import com.hrpm.mapper.EmployeeMapper;
import com.hrpm.mapper.PerformanceConfigurationMapper;
import com.hrpm.mapper.UserAccountMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PerformanceExecutionServiceTests {
    private final PerformanceConfigurationMapper performanceMapper = Mockito.mock(PerformanceConfigurationMapper.class);
    private final UserAccountMapper userAccountMapper = Mockito.mock(UserAccountMapper.class);
    private final PerformanceExecutionService service = new PerformanceExecutionService(
            performanceMapper, userAccountMapper, Mockito.mock(EmployeeMapper.class), Mockito.mock(IdGenerator.class));

    @Test
    void selfAssessmentRejectsAnotherEmployeesTask() {
        when(performanceMapper.findTask(10L)).thenReturn(task(20L, "PENDING_SELF_ASSESSMENT"));
        when(userAccountMapper.findById(1L)).thenReturn(account(30L));

        assertThrows(IllegalStateException.class, () -> service.submitSelfAssessment(1L, 10L, submission(100L)));
    }

    @Test
    void selfAssessmentRequiresEveryTaskItemToBeScored() {
        when(performanceMapper.findTask(10L)).thenReturn(task(20L, "PENDING_SELF_ASSESSMENT"));
        when(userAccountMapper.findById(1L)).thenReturn(account(20L));
        when(performanceMapper.listTaskItems(10L)).thenReturn(List.of(
                new TaskItem(100L, 10L, "{}", BigDecimal.valueOf(50), "[]", 0),
                new TaskItem(101L, 10L, "{}", BigDecimal.valueOf(50), "[]", 0)));

        assertThrows(IllegalArgumentException.class, () -> service.submitSelfAssessment(1L, 10L, submission(100L)));
    }

    @Test
    void managerScoreRejectsUsersWhoAreNotTheDirectManager() {
        when(performanceMapper.findTask(10L)).thenReturn(task(20L, "PENDING_MANAGER_SCORE"));
        when(userAccountMapper.findById(2L)).thenReturn(account(99L));

        assertThrows(IllegalStateException.class, () -> service.submitManagerScore(2L, 10L, submission(100L)));
    }

    @Test
    void unlinkedAdministratorHasNoPersonalPerformanceTasks() {
        when(userAccountMapper.findById(1L)).thenReturn(new UserAccount(1L, "admin", "hash", null, "ACTIVE", 0));

        assertTrue(service.listMyTasks(1L).isEmpty());
        assertTrue(service.listManagerTasks(1L).isEmpty());
    }

    private Task task(long employeeId, String status) {
        return new Task(10L, 1L, employeeId, 88L, 2L, "{}", status, 0);
    }

    private UserAccount account(long employeeId) {
        return new UserAccount(1L, "user", "hash", employeeId, "ACTIVE", 0);
    }

    private SubmitScores submission(long itemId) {
        return new SubmitScores(0, List.of(new ScoreItem(itemId, BigDecimal.valueOf(80), "ok")));
    }
}
