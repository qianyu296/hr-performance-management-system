package com.hrpm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpm.common.IdGenerator;
import com.hrpm.common.exception.DuplicateResourceException;
import com.hrpm.common.exception.InvalidPerformanceSchemeException;
import com.hrpm.common.exception.ResourceNotFoundException;
import com.hrpm.common.exception.VersionConflictException;
import com.hrpm.dto.PerformanceConfigurationDTOs.CycleAction;
import com.hrpm.dto.PerformanceConfigurationDTOs.Item;
import com.hrpm.dto.PerformanceConfigurationDTOs.Metric;
import com.hrpm.dto.PerformanceConfigurationDTOs.MetricUpdate;
import com.hrpm.dto.PerformanceConfigurationDTOs.Rule;
import com.hrpm.dto.PerformanceConfigurationDTOs.Version;
import com.hrpm.dto.PerformanceConfigurationDTOs.VersionAction;
import com.hrpm.dto.PerformanceConfigurationDTOs.VersionUpdate;
import com.hrpm.entity.Employee;
import com.hrpm.entity.PerformanceMetric;
import com.hrpm.entity.PerformanceConfigurationModels.Cycle;
import com.hrpm.entity.PerformanceConfigurationModels.LevelRuleRow;
import com.hrpm.entity.PerformanceConfigurationModels.Scheme;
import com.hrpm.entity.PerformanceConfigurationModels.SchemeItem;
import com.hrpm.entity.PerformanceConfigurationModels.SchemeVersion;
import com.hrpm.mapper.EmployeeMapper;
import com.hrpm.mapper.PerformanceConfigurationMapper;
import com.hrpm.vo.PerformanceConfigurationVOs.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerformanceConfigurationService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private final PerformanceConfigurationMapper mapper;
    private final EmployeeMapper employeeMapper;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public PerformanceConfigurationService(PerformanceConfigurationMapper mapper, EmployeeMapper employeeMapper,
                                           IdGenerator idGenerator, ObjectMapper objectMapper) {
        this.mapper = mapper; this.employeeMapper = employeeMapper; this.idGenerator = idGenerator; this.objectMapper = objectMapper;
    }

    public List<MetricVO> listMetrics() { return mapper.listMetrics().stream().map(MetricVO::from).toList(); }
    @Transactional public MetricVO createMetric(Metric request) {
        String code = normalizedCode(request.code());
        if (mapper.findMetricByCode(code) != null) throw new DuplicateResourceException("Performance metric code already exists");
        validateJson(request.scoreConfig(), "score configuration");
        long id = idGenerator.nextId();
        mapper.insertMetric(id, code, request.name().trim(), request.metricType().trim(), nullable(request.unit()), request.scoreMethod().trim(), request.scoreConfig(), nullable(request.description()), request.status().trim());
        return MetricVO.from(requireMetric(id));
    }
    @Transactional public MetricVO updateMetric(long id, MetricUpdate request) {
        requireMetric(id); validateJson(request.scoreConfig(), "score configuration");
        if (mapper.updateMetric(id, request.name().trim(), request.metricType().trim(), nullable(request.unit()), request.scoreMethod().trim(), request.scoreConfig(), nullable(request.description()), request.status().trim(), request.version()) != 1) throw new VersionConflictException();
        return MetricVO.from(requireMetric(id));
    }

    public List<SchemeVO> listSchemes() { return mapper.listSchemes().stream().map(this::schemeVO).toList(); }
    @Transactional public SchemeVO createScheme(com.hrpm.dto.PerformanceConfigurationDTOs.Scheme request) {
        String code = normalizedCode(request.code());
        if (mapper.findSchemeByCode(code) != null) throw new DuplicateResourceException("Performance scheme code already exists");
        validateJson(request.applicabilityRule(), "applicability rule");
        long id = idGenerator.nextId(); mapper.insertScheme(id, code, request.name().trim(), request.applicabilityRule());
        return schemeVO(requireScheme(id));
    }
    @Transactional public VersionVO createVersion(long schemeId, Version request) {
        Scheme scheme = requireScheme(schemeId); validateConfiguration(request.evaluationStages(), request.items(), request.levelRules());
        int versionNo = mapper.listVersions(scheme.id()).stream().mapToInt(SchemeVersion::versionNo).max().orElse(0) + 1;
        long id = idGenerator.nextId(); mapper.insertVersion(id, scheme.id(), versionNo, request.evaluationStages(), snapshot(request.evaluationStages(), request.items(), request.levelRules()));
        replaceConfiguration(id, request.items(), request.levelRules()); return versionVO(requireVersion(id));
    }
    @Transactional public VersionVO updateVersion(long id, VersionUpdate request) {
        SchemeVersion current = requireVersion(id); if (!"DRAFT".equals(current.status())) throw new IllegalStateException("Enabled scheme versions cannot be changed");
        validateConfiguration(request.evaluationStages(), request.items(), request.levelRules());
        if (mapper.updateVersion(id, request.evaluationStages(), snapshot(request.evaluationStages(), request.items(), request.levelRules()), request.version()) != 1) throw new VersionConflictException();
        mapper.deleteItems(id); mapper.deleteRules(id); replaceConfiguration(id, request.items(), request.levelRules()); return versionVO(requireVersion(id));
    }
    @Transactional public VersionVO enableVersion(long id, VersionAction request) {
        SchemeVersion version = requireVersion(id); if (!"DRAFT".equals(version.status())) throw new IllegalStateException("Scheme version is already enabled");
        validateStoredConfiguration(version);
        if (mapper.enableVersion(id, request.version()) != 1) throw new VersionConflictException();
        return versionVO(requireVersion(id));
    }

    public List<CycleVO> listCycles() { return mapper.listCycles().stream().map(CycleVO::from).toList(); }
    @Transactional public CycleVO createCycle(com.hrpm.dto.PerformanceConfigurationDTOs.Cycle request) {
        String code = normalizedCode(request.code());
        if (mapper.findCycleByCode(code) != null) throw new DuplicateResourceException("Performance cycle code already exists");
        SchemeVersion version = requireVersion(request.schemeVersionId()); if (!"ENABLED".equals(version.status())) throw new InvalidPerformanceSchemeException("Performance cycle requires an enabled scheme version");
        validateCycleDates(request.startDate(), request.endDate(), request.selfDeadline(), request.managerDeadline()); validateJson(request.applicabilityRule(), "applicability rule");
        long id = idGenerator.nextId(); mapper.insertCycle(id, code, request.name().trim(), version.id(), request.startDate(), request.endDate(), request.selfDeadline(), request.managerDeadline(), request.appealDeadline(), request.applicabilityRule());
        return CycleVO.from(requireCycle(id));
    }
    @Transactional public CycleVO startCycle(long id, CycleAction request) {
        Cycle cycle = requireCycle(id); SchemeVersion version = requireVersion(cycle.schemeVersionId());
        if (!"ENABLED".equals(version.status())) throw new InvalidPerformanceSchemeException("Performance cycle requires an enabled scheme version");
        if (mapper.startCycle(id, request.version()) != 1) throw new VersionConflictException();
        List<SchemeItem> items = mapper.listItems(version.id());
        for (long employeeId : employeeMapper.listActiveIds()) {
            Employee employee = employeeMapper.findById(employeeId);
            long taskId = idGenerator.nextId();
            mapper.insertTask(taskId, cycle.id(), employee.id(), employee.managerEmployeeId(), version.id(), json(Map.of("employeeId", employee.id(), "employeeNo", employee.employeeNo(), "departmentId", employee.departmentId(), "departmentName", employee.departmentName(), "positionId", employee.positionId(), "positionName", employee.positionName())));
            for (SchemeItem item : items) {
                PerformanceMetric metric = requireMetric(item.metricId());
                mapper.insertTaskItem(idGenerator.nextId(), taskId, json(Map.of("metricId", metric.id(), "code", metric.code(), "name", metric.name(), "scoreMethod", item.scoreMethod(), "scoreConfig", item.scoreConfig())), item.weight(), version.evaluationStages());
            }
        }
        return CycleVO.from(requireCycle(id));
    }

    private SchemeVO schemeVO(Scheme scheme) { return new SchemeVO(Long.toString(scheme.id()), scheme.code(), scheme.name(), scheme.applicabilityRule(), scheme.status(), scheme.version(), mapper.listVersions(scheme.id()).stream().map(this::versionVO).toList()); }
    private VersionVO versionVO(SchemeVersion value) { return new VersionVO(Long.toString(value.id()), Long.toString(value.schemeId()), value.versionNo(), value.evaluationStages(), value.status(), value.version(), mapper.listItems(value.id()).stream().map(ItemVO::from).toList(), mapper.listRules(value.id()).stream().map(RuleVO::from).toList()); }
    private void replaceConfiguration(long versionId, List<Item> items, List<Rule> rules) { for (Item item : items) mapper.insertItem(idGenerator.nextId(), versionId, item.metricId(), item.weight(), item.scoreMethod(), item.scoreConfig(), item.sortNo()); for (Rule rule : rules) mapper.insertRule(idGenerator.nextId(), versionId, rule.levelCode().trim(), rule.minScore(), rule.maxScore(), rule.includeMin(), rule.includeMax()); }
    private void validateConfiguration(String stages, List<Item> items, List<Rule> rules) {
        validateJson(stages, "evaluation stages"); BigDecimal weight = items.stream().map(Item::weight).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (weight.compareTo(ONE_HUNDRED) != 0) throw new InvalidPerformanceSchemeException("Performance scheme weights must total 100");
        if (items.stream().map(Item::metricId).distinct().count() != items.size()) throw new InvalidPerformanceSchemeException("A metric can appear only once in a scheme version");
        for (Item item : items) { PerformanceMetric metric = requireMetric(item.metricId()); if (!"ACTIVE".equals(metric.status())) throw new InvalidPerformanceSchemeException("Scheme version can only use active metrics"); validateJson(item.scoreConfig(), "score configuration"); }
        List<Rule> ordered = rules.stream().sorted(Comparator.comparing(Rule::minScore)).toList();
        for (int i = 0; i < ordered.size(); i++) { Rule rule = ordered.get(i); if (rule.minScore().compareTo(rule.maxScore()) > 0) throw new InvalidPerformanceSchemeException("Level rule minimum cannot exceed maximum"); if (i > 0) { Rule previous = ordered.get(i - 1); int boundary = previous.maxScore().compareTo(rule.minScore()); if (boundary > 0 || boundary == 0 && previous.includeMax() && rule.includeMin()) throw new InvalidPerformanceSchemeException("Performance level rules cannot overlap"); } }
    }
    private void validateStoredConfiguration(SchemeVersion version) { List<SchemeItem> items = mapper.listItems(version.id()); List<LevelRuleRow> rules = mapper.listRules(version.id()); if (items.isEmpty() || rules.isEmpty()) throw new InvalidPerformanceSchemeException("Performance scheme version requires items and level rules"); BigDecimal weight = items.stream().map(SchemeItem::weight).reduce(BigDecimal.ZERO, BigDecimal::add); if (weight.compareTo(ONE_HUNDRED) != 0) throw new InvalidPerformanceSchemeException("Performance scheme weights must total 100"); }
    private void validateCycleDates(java.time.LocalDate start, java.time.LocalDate end, LocalDateTime self, LocalDateTime manager) { if (start.isAfter(end) || self.isAfter(manager)) throw new InvalidPerformanceSchemeException("Performance cycle dates are invalid"); }
    private PerformanceMetric requireMetric(long id) { PerformanceMetric metric = mapper.findMetric(id); if (metric == null) throw new ResourceNotFoundException("Performance metric not found"); return metric; }
    private Scheme requireScheme(long id) { Scheme scheme = mapper.findScheme(id); if (scheme == null) throw new ResourceNotFoundException("Performance scheme not found"); return scheme; }
    private SchemeVersion requireVersion(long id) { SchemeVersion version = mapper.findVersion(id); if (version == null) throw new ResourceNotFoundException("Performance scheme version not found"); return version; }
    private Cycle requireCycle(long id) { Cycle cycle = mapper.findCycle(id); if (cycle == null) throw new ResourceNotFoundException("Performance cycle not found"); return cycle; }
    private String snapshot(String stages, List<Item> items, List<Rule> rules) { return json(Map.of("evaluationStages", stages, "items", items, "levelRules", rules)); }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException exception) { throw new IllegalArgumentException("Unable to serialize performance configuration"); } }
    private void validateJson(String value, String label) { try { objectMapper.readTree(value); } catch (Exception exception) { throw new IllegalArgumentException("Invalid " + label + " JSON"); } }
    private String normalizedCode(String value) { return value.trim().toUpperCase(Locale.ROOT); }
    private String nullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
