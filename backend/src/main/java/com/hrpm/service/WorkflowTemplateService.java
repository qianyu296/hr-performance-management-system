package com.hrpm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpm.common.IdGenerator;
import com.hrpm.common.exception.DuplicateResourceException;
import com.hrpm.common.exception.OrganizationReferenceInvalidException;
import com.hrpm.common.exception.ResourceNotFoundException;
import com.hrpm.common.exception.VersionConflictException;
import com.hrpm.dto.CreateWorkflowTemplateDTO;
import com.hrpm.dto.UpdateWorkflowTemplateDTO;
import com.hrpm.dto.WorkflowTemplateNodeDTO;
import com.hrpm.entity.WorkflowTemplateConfig;
import com.hrpm.entity.WorkflowTemplateDefinition;
import com.hrpm.entity.WorkflowTemplateNode;
import com.hrpm.mapper.WorkflowMapper;
import com.hrpm.vo.WorkflowTemplateVO;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowTemplateService {
    private static final Set<String> BUSINESS_TYPES = Set.of("LEAVE", "OVERTIME", "PERSONNEL_CHANGE", "PERFORMANCE_APPEAL");
    private static final Set<String> NODE_TYPES = Set.of("SPECIFIC_USER", "DIRECT_MANAGER", "DEPARTMENT_LEADER", "HR");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");

    private final WorkflowMapper workflowMapper;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public WorkflowTemplateService(WorkflowMapper workflowMapper, IdGenerator idGenerator, ObjectMapper objectMapper) {
        this.workflowMapper = workflowMapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    public List<WorkflowTemplateVO> list() {
        return workflowMapper.findTemplates().stream().map(this::loadConfig).map(this::toView).toList();
    }

    public WorkflowTemplateVO get(long id) {
        return toView(loadConfig(requireTemplate(id)));
    }

    @Transactional
    public WorkflowTemplateVO create(CreateWorkflowTemplateDTO request) {
        validate(request.businessType(), request.status(), request.departmentIds(), request.nodes());
        if (workflowMapper.countByCodeAndVersion(request.businessType(), request.code(), request.templateVersion()) > 0) {
            throw new DuplicateResourceException("Workflow template code and version already exist");
        }
        long templateId = idGenerator.nextId();
        workflowMapper.insertTemplate(templateId, request.code(), request.name(), request.businessType(), request.priority(), request.templateVersion(), request.status());
        replaceScopesAndNodes(templateId, request.departmentIds(), request.nodes());
        return get(templateId);
    }

    @Transactional
    public WorkflowTemplateVO update(long id, UpdateWorkflowTemplateDTO request) {
        WorkflowTemplateDefinition current = requireTemplate(id);
        int version = parseVersion(request.version());
        validate(request.businessType(), request.status(), request.departmentIds(), request.nodes());
        if (workflowMapper.updateTemplate(id, request.name(), request.businessType(), request.priority(), request.status(), version) != 1) {
            throw new VersionConflictException();
        }
        replaceScopesAndNodes(id, request.departmentIds(), request.nodes());
        return get(current.id());
    }

    private void replaceScopesAndNodes(long templateId, List<String> departmentIds, List<WorkflowTemplateNodeDTO> nodes) {
        workflowMapper.deleteScopes(templateId);
        workflowMapper.deleteNodes(templateId);
        for (String departmentId : departmentIds) {
            workflowMapper.insertScope(idGenerator.nextId(), templateId, Long.parseLong(departmentId));
        }
        for (WorkflowTemplateNodeDTO node : nodes) {
            workflowMapper.insertNode(idGenerator.nextId(), templateId, node.nodeNo(), node.nodeType(), node.approverRule().toString());
        }
    }

    private WorkflowTemplateDefinition requireTemplate(long id) {
        WorkflowTemplateDefinition template = workflowMapper.findTemplateById(id);
        if (template == null) {
            throw new ResourceNotFoundException("Workflow template not found");
        }
        return template;
    }

    private WorkflowTemplateConfig loadConfig(WorkflowTemplateDefinition template) {
        return new WorkflowTemplateConfig(template, workflowMapper.findScopeDepartmentIds(template.id()), workflowMapper.findNodes(template.id()));
    }

    private WorkflowTemplateVO toView(WorkflowTemplateConfig config) {
        return WorkflowTemplateVO.from(config, this::parseRule);
    }

    private JsonNode parseRule(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Workflow template contains an invalid approver rule", exception);
        }
    }

    private void validate(String businessType, String status, List<String> departmentIds, List<WorkflowTemplateNodeDTO> nodes) {
        if (!BUSINESS_TYPES.contains(businessType) || !STATUSES.contains(status)) {
            throw new OrganizationReferenceInvalidException("Unsupported workflow template type or status");
        }
        try {
            for (String departmentId : departmentIds) {
                long id = Long.parseLong(departmentId);
                if (workflowMapper.countActiveDepartment(id) != 1) {
                    throw new OrganizationReferenceInvalidException("Workflow scope department is missing or inactive");
                }
            }
        } catch (NumberFormatException exception) {
            throw new OrganizationReferenceInvalidException("Invalid workflow scope department ID");
        }
        for (int index = 0; index < nodes.size(); index++) {
            WorkflowTemplateNodeDTO node = nodes.get(index);
            if (node.nodeNo() != index + 1 || !NODE_TYPES.contains(node.nodeType()) || !node.approverRule().isObject()) {
                throw new OrganizationReferenceInvalidException("Workflow nodes must be contiguous and use supported approver rules");
            }
            validateApproverRule(node);
        }
    }

    private void validateApproverRule(WorkflowTemplateNodeDTO node) {
        String configuredType = node.approverRule().path("type").asText(node.nodeType());
        if (!configuredType.equals(node.nodeType())) {
            throw new OrganizationReferenceInvalidException("Approver rule type must match the workflow node type");
        }
        if (!"SPECIFIC_USER".equals(node.nodeType())) {
            return;
        }
        JsonNode userId = node.approverRule().get("userId");
        if (userId == null || !userId.canConvertToLong() || workflowMapper.findActiveUserId(userId.longValue()) == null) {
            throw new OrganizationReferenceInvalidException("Specific approver user is missing or inactive");
        }
    }

    private int parseVersion(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new OrganizationReferenceInvalidException("Invalid workflow template version");
        }
    }
}
