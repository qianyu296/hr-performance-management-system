package com.hrpm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrpm.common.IdGenerator;
import com.hrpm.common.TraceIdContext;
import com.hrpm.entity.OperationAuditLog;
import com.hrpm.mapper.OperationAuditMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OperationAuditService {
    private final OperationAuditMapper operationAuditMapper;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public OperationAuditService(OperationAuditMapper operationAuditMapper, IdGenerator idGenerator, ObjectMapper objectMapper) {
        this.operationAuditMapper = operationAuditMapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    public void recordSuccess(String module, String action, String objectType, Long objectId, Long actorUserId, Object summary) {
        insert(module, action, objectType, objectId, actorUserId, "SUCCESS", summary);
    }

    public void recordFailure(String module, String action, String objectType, Long objectId, Long actorUserId, Object summary) {
        insert(module, action, objectType, objectId, actorUserId, "FAILURE", summary);
    }

    private void insert(String module, String action, String objectType, Long objectId, Long actorUserId,
                        String result, Object summary) {
        Object sanitized = sanitize(summary);
        String summaryJson = toJson(sanitized);
        OperationAuditLog log = new OperationAuditLog(
                idGenerator.nextId(),
                actorUserId,
                module,
                objectType,
                objectId,
                action,
                result,
                TraceIdContext.current(),
                summaryJson,
                sourceAddress(),
                LocalDateTime.now());
        if (operationAuditMapper.insert(log, summaryJson) != 1) {
            throw new IllegalStateException("Unable to write operation audit log");
        }
    }

    private Object sanitize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (isSensitiveKey(key)) {
                    continue;
                }
                sanitized.put(key, sanitize(entry.getValue()));
            }
            return sanitized;
        }
        if (value instanceof List<?> list) {
            List<Object> sanitized = new ArrayList<>(list.size());
            for (Object item : list) {
                sanitized.add(sanitize(item));
            }
            return sanitized;
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        return sanitize(objectMapper.convertValue(value, Object.class));
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("password") || normalized.contains("secret") || normalized.contains("token");
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize audit summary", exception);
        }
    }

    private String sourceAddress() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            return request == null ? null : request.getRemoteAddr();
        }
        return null;
    }
}
