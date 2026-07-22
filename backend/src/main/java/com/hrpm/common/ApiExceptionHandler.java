package com.hrpm.common;

import com.hrpm.common.exception.AuthenticationFailedException;
import com.hrpm.common.exception.DataScopeDeniedException;
import com.hrpm.common.exception.DuplicateResourceException;
import com.hrpm.common.exception.IllegalLeaveStateTransitionException;
import com.hrpm.common.exception.InsufficientLeaveBalanceException;
import com.hrpm.common.exception.InvalidPerformanceSchemeException;
import com.hrpm.common.exception.OrganizationReferenceInvalidException;
import com.hrpm.common.exception.ResourceNotFoundException;
import com.hrpm.common.exception.TokenValidationException;
import com.hrpm.common.exception.VersionConflictException;
import com.hrpm.common.exception.WorkflowTaskInvalidException;
import com.hrpm.common.exception.WorkflowTemplateMissingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiResponse<Void>> handleRequestValidation(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Invalid request");
    }

    @ExceptionHandler(OrganizationReferenceInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOrganizationReference(OrganizationReferenceInvalidException exception) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationFailure(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", exception.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(VersionConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleVersionConflict(VersionConflictException exception) {
        return error(HttpStatus.CONFLICT, "VERSION_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(DuplicateResourceException exception) {
        return error(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(DataScopeDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataScopeDenied(DataScopeDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "DATA_SCOPE_DENIED", exception.getMessage());
    }

    @ExceptionHandler(WorkflowTaskInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handleWorkflowTaskInvalid(WorkflowTaskInvalidException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "WORKFLOW_TASK_INVALID", exception.getMessage());
    }

    @ExceptionHandler(WorkflowTemplateMissingException.class)
    public ResponseEntity<ApiResponse<Void>> handleWorkflowTemplateMissing(WorkflowTemplateMissingException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "WORKFLOW_TEMPLATE_MISSING", exception.getMessage());
    }

    @ExceptionHandler(InsufficientLeaveBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientLeaveBalance(InsufficientLeaveBalanceException exception) {
        return error(HttpStatus.CONFLICT, "LEAVE_BALANCE_INSUFFICIENT", exception.getMessage());
    }

    @ExceptionHandler(IllegalLeaveStateTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalLeaveStateTransition(IllegalLeaveStateTransitionException exception) {
        return error(HttpStatus.CONFLICT, "LEAVE_STATE_INVALID", exception.getMessage());
    }

    @ExceptionHandler(InvalidPerformanceSchemeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPerformanceScheme(InvalidPerformanceSchemeException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "PERFORMANCE_SCHEME_INVALID", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleStateConflict(IllegalStateException exception) {
        return error(HttpStatus.CONFLICT, "STATE_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationFailed(AuthenticationFailedException exception) {
        return error(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", exception.getMessage());
    }

    @ExceptionHandler(TokenValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidToken(TokenValidationException exception) {
        return error(HttpStatus.UNAUTHORIZED, "AUTH_SESSION_INVALID", exception.getMessage());
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(code, ExceptionMessageLocalizer.localize(message), null, TraceIdContext.current()));
    }
}
