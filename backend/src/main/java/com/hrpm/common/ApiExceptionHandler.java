package com.hrpm.common;


import com.hrpm.common.exception.AuthenticationFailedException;
import com.hrpm.common.exception.WorkflowTaskInvalidException;
import com.hrpm.common.exception.WorkflowTemplateMissingException;
import com.hrpm.common.exception.DuplicateResourceException;
import com.hrpm.common.exception.DataScopeDeniedException;
import com.hrpm.common.exception.VersionConflictException;
import com.hrpm.common.exception.OrganizationReferenceInvalidException;
import com.hrpm.common.exception.ResourceNotFoundException;
import com.hrpm.common.exception.TokenValidationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(OrganizationReferenceInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOrganizationReference(OrganizationReferenceInvalidException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>("VALIDATION_FAILED", exception.getMessage(), null, TraceIdContext.current()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationFailure(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>("VALIDATION_FAILED", exception.getMessage(), null, TraceIdContext.current()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>("RESOURCE_NOT_FOUND", exception.getMessage(), null, TraceIdContext.current()));
    }

    @ExceptionHandler(VersionConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleVersionConflict(VersionConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>("VERSION_CONFLICT", exception.getMessage(), null, TraceIdContext.current()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(DuplicateResourceException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>("IDEMPOTENCY_CONFLICT", exception.getMessage(), null, TraceIdContext.current()));
    }

    @ExceptionHandler(DataScopeDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataScopeDenied(DataScopeDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>("DATA_SCOPE_DENIED", exception.getMessage(), null, TraceIdContext.current()));
    }

    @ExceptionHandler(WorkflowTaskInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handleWorkflowTaskInvalid(WorkflowTaskInvalidException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiResponse<>("WORKFLOW_TASK_INVALID", exception.getMessage(), null, TraceIdContext.current()));
    }
    @ExceptionHandler(WorkflowTemplateMissingException.class)
    public ResponseEntity<ApiResponse<Void>> handleWorkflowTemplateMissing(WorkflowTemplateMissingException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiResponse<>("WORKFLOW_TEMPLATE_MISSING", exception.getMessage(), null, TraceIdContext.current()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleStateConflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>("STATE_CONFLICT", exception.getMessage(), null, TraceIdContext.current()));
    }
    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationFailed(AuthenticationFailedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>("AUTH_INVALID_CREDENTIALS", "Username or password is invalid", null, TraceIdContext.current()));
    }

    @ExceptionHandler(TokenValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidToken(TokenValidationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>("AUTH_SESSION_INVALID", "Session token is invalid or expired", null, TraceIdContext.current()));
    }
}
