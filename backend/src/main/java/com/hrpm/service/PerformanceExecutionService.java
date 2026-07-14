package com.hrpm.service;

import com.hrpm.common.IdGenerator;
import com.hrpm.common.exception.ResourceNotFoundException;
import com.hrpm.common.exception.VersionConflictException;
import com.hrpm.dto.PerformanceExecutionDTOs.SubmitScores;
import com.hrpm.entity.LevelRule;
import com.hrpm.entity.PerformanceConfigurationModels.*;
import com.hrpm.entity.PerformanceScore;
import com.hrpm.entity.ScoreItem;
import com.hrpm.entity.UserAccount;
import com.hrpm.mapper.PerformanceConfigurationMapper;
import com.hrpm.mapper.EmployeeMapper;
import com.hrpm.mapper.UserAccountMapper;
import com.hrpm.vo.PerformanceExecutionVOs.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerformanceExecutionService {
    private final PerformanceConfigurationMapper mapper;
    private final UserAccountMapper userAccountMapper;
    private final EmployeeMapper employeeMapper;
    private final IdGenerator idGenerator;
    private final PerformanceScoringService scoringService = new PerformanceScoringService();

    public PerformanceExecutionService(PerformanceConfigurationMapper mapper, UserAccountMapper userAccountMapper,
                                       EmployeeMapper employeeMapper, IdGenerator idGenerator) {
        this.mapper = mapper; this.userAccountMapper = userAccountMapper; this.employeeMapper = employeeMapper; this.idGenerator = idGenerator;
    }

    public List<TaskVO> listMyTasks(long userId) {
        UserAccount account = userAccountMapper.findById(userId);
        if (account == null || account.employeeId() == null) return List.of();
        return mapper.listEmployeeTasks(userId).stream().map(this::taskVO).toList();
    }
    public List<TaskVO> listManagerTasks(long userId) {
        UserAccount account = userAccountMapper.findById(userId);
        if (account == null || account.employeeId() == null) return List.of();
        long employeeId = account.employeeId();
        return mapper.listManagerTasks(employeeId).stream().map(this::taskVO).toList();
    }
    @Transactional public TaskVO submitSelfAssessment(long userId, long taskId, SubmitScores request) {
        Task task = requireTask(taskId); long employeeId = employeeId(userId);
        if (task.employeeId() != employeeId || !"PENDING_SELF_ASSESSMENT".equals(task.status())) throw new IllegalStateException("Task cannot be self-assessed by this user");
        submitScores(task, employeeId, "SELF", request);
        if (mapper.advanceTask(task.id(), "PENDING_SELF_ASSESSMENT", "PENDING_MANAGER_SCORE", request.version()) != 1) throw new VersionConflictException();
        return taskVO(requireTask(taskId));
    }
    @Transactional public TaskVO submitManagerScore(long userId, long taskId, SubmitScores request) {
        Task task = requireTask(taskId); long employeeId = employeeId(userId);
        if (!Objects.equals(task.managerEmployeeId(), employeeId) || !"PENDING_MANAGER_SCORE".equals(task.status())) throw new IllegalStateException("Task cannot be scored by this manager");
        submitScores(task, employeeId, "MANAGER", request);
        PerformanceScore score = managerScore(task, employeeId);
        long resultId = idGenerator.nextId(); mapper.insertResult(resultId, task.id()); mapper.insertResultVersion(idGenerator.nextId(), resultId, score.totalScore(), score.levelCode(), "Manager score submitted");
        if (mapper.advanceTask(task.id(), "PENDING_MANAGER_SCORE", "PENDING_PUBLISH", request.version()) != 1) throw new VersionConflictException();
        return taskVO(requireTask(taskId));
    }
    @Transactional public int publishCycle(long userId, long cycleId, int version) {
        Cycle cycle = requireCycle(cycleId); if (!"STARTED".equals(cycle.status())) throw new IllegalStateException("Only a started cycle can be published");
        List<Task> tasks = mapper.listPublishableTasks(cycleId); if (tasks.isEmpty()) throw new IllegalStateException("There are no completed tasks to publish");
        if (mapper.publishCycle(cycleId, version) != 1) throw new VersionConflictException();
        for (Task task : tasks) {
            Result result = mapper.findResult(task.id()); ResultVersion resultVersion = mapper.findLatestResultVersion(result.id());
            mapper.markResultVersionPublished(resultVersion.id(), userId); mapper.publishResult(result.id());
            if (mapper.advanceTask(task.id(), "PENDING_PUBLISH", "PUBLISHED", task.version()) != 1) throw new VersionConflictException();
        }
        return tasks.size();
    }

    private void submitScores(Task task, long evaluatorId, String stage, SubmitScores request) {
        List<TaskItem> taskItems = mapper.listTaskItems(task.id());
        Map<Long, com.hrpm.dto.PerformanceExecutionDTOs.ScoreItem> submitted = new HashMap<>();
        request.items().forEach(item -> { if (submitted.put(item.taskItemId(), item) != null) throw new IllegalArgumentException("A task item can only be scored once"); });
        if (submitted.size() != taskItems.size() || taskItems.stream().anyMatch(item -> !submitted.containsKey(item.id()))) throw new IllegalArgumentException("Every task item must be scored");
        for (TaskItem item : taskItems) {
            if (mapper.findScore(item.id(), stage, evaluatorId) != null) throw new IllegalStateException("Scores have already been submitted");
            var value = submitted.get(item.id()); BigDecimal weighted = value.rawScore().multiply(item.weight()).divide(new BigDecimal("100"), 8, java.math.RoundingMode.HALF_UP);
            mapper.insertScore(idGenerator.nextId(), item.id(), stage, evaluatorId, value.rawScore(), weighted, value.comment());
        }
    }
    private PerformanceScore managerScore(Task task, long managerEmployeeId) {
        List<ScoreItem> items = mapper.listTaskItems(task.id()).stream().map(item -> {
            Score score = mapper.findScore(item.id(), "MANAGER", managerEmployeeId);
            return new ScoreItem(Long.toString(item.id()), item.weight(), score.rawScore());
        }).toList();
        List<LevelRule> rules = mapper.listRules(task.schemeVersionId()).stream().map(rule -> new LevelRule(rule.levelCode(), rule.minScore(), rule.maxScore(), rule.includeMin(), rule.includeMax())).toList();
        return scoringService.score(items, rules);
    }
    private TaskVO taskVO(Task task) {
        Cycle cycle = requireCycle(task.cycleId()); var employee = requireEmployee(task.employeeId());
        Result result = mapper.findResult(task.id()); ResultVersion latest = result == null ? null : mapper.findLatestResultVersion(result.id());
        List<TaskItemVO> items = mapper.listTaskItems(task.id()).stream().map(item -> new TaskItemVO(Long.toString(item.id()), item.metricSnapshot(), item.weight(), item.version(), mapper.listScores(item.id()).stream().map(score -> new ScoreVO(score.stage(), plain(score.rawScore()), plain(score.weightedScore()), score.comment())).toList())).toList();
        return new TaskVO(Long.toString(task.id()), Long.toString(cycle.id()), cycle.name(), Long.toString(task.employeeId()), employee.name(), task.managerEmployeeId() == null ? null : Long.toString(task.managerEmployeeId()), task.status(), task.version(), items, latest == null ? null : plain(latest.totalScore()), latest == null ? null : latest.levelCode(), result == null ? null : result.publishStatus());
    }
    private long employeeId(long userId) { UserAccount account = userAccountMapper.findById(userId); if (account == null || account.employeeId() == null) throw new ResourceNotFoundException("Current user is not linked to an employee"); return account.employeeId(); }
    private Task requireTask(long id) { Task task = mapper.findTask(id); if (task == null) throw new ResourceNotFoundException("Performance task not found"); return task; }
    private Cycle requireCycle(long id) { Cycle cycle = mapper.findCycle(id); if (cycle == null) throw new ResourceNotFoundException("Performance cycle not found"); return cycle; }
    private com.hrpm.entity.Employee requireEmployee(long id) { com.hrpm.entity.Employee employee = employeeMapper.findById(id); if (employee == null) throw new ResourceNotFoundException("Employee not found"); return employee; }
    private String plain(BigDecimal value) { return value == null ? null : value.stripTrailingZeros().toPlainString(); }
}
