package com.novastack.sms.exception;

import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.provider.ProviderErrorMessages;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Locale;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.fail(HumanReadableErrors.fromException(ex), ex.getData()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::fieldMessage)
                .collect(Collectors.joining("; "));
        return fail(HttpStatus.BAD_REQUEST, blankToGeneric(message, "Please check the form and try again."));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBind(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::fieldMessage)
                .collect(Collectors.joining("; "));
        return fail(HttpStatus.BAD_REQUEST, blankToGeneric(message, "Please check the form and try again."));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("; "));
        return fail(HttpStatus.BAD_REQUEST, blankToGeneric(message, "Please check the request and try again."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Unreadable request body: {}", ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage());
        return fail(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        return fail(HttpStatus.BAD_REQUEST, "Missing required parameter: " + ex.getParameterName() + ".");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String name = ex.getName() == null ? "value" : ex.getName();
        return fail(HttpStatus.BAD_REQUEST, "Invalid value for " + name + ".");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return fail(HttpStatus.METHOD_NOT_ALLOWED, "This HTTP method is not allowed for this URL.");
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception ex) {
        return fail(HttpStatus.NOT_FOUND, "The requested URL was not found.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return fail(HttpStatus.PAYLOAD_TOO_LARGE, "The uploaded file is too large.");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(EntityNotFoundException ex) {
        return fail(HttpStatus.NOT_FOUND, HumanReadableErrors.fromException(ex));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return fail(HttpStatus.UNAUTHORIZED, "Invalid credentials.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return fail(HttpStatus.FORBIDDEN, "You do not have permission to do this.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        return fail(HttpStatus.UNAUTHORIZED, "Please sign in to continue.");
    }

    @ExceptionHandler({WebClientResponseException.class, WebClientRequestException.class})
    public ResponseEntity<ApiResponse<Void>> handleProvider(Exception ex) {
        String vendor = null;
        if (ex instanceof WebClientResponseException web) {
            vendor = HumanReadableErrors.fromVendor(web.getResponseBodyAsString());
            log.error("SMS provider HTTP error status={} body={}",
                    web.getStatusCode().value(),
                    web.getResponseBodyAsString());
        } else {
            log.error("SMS provider HTTP error: {}", ex.getMessage());
        }
        if (vendor != null) {
            return fail(HttpStatus.BAD_GATEWAY, vendor);
        }
        return fail(HttpStatus.BAD_GATEWAY, ProviderErrorMessages.UNAVAILABLE);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String detail = "";
        if (ex.getMostSpecificCause() != null && ex.getMostSpecificCause().getMessage() != null) {
            detail = ex.getMostSpecificCause().getMessage().toLowerCase(Locale.ROOT);
        }
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage());
        if (detail.contains("uk_users_email") || detail.contains("uk_org_email")) {
            return fail(HttpStatus.CONFLICT, "Email already registered.");
        }
        if (detail.contains("phone")) {
            return fail(HttpStatus.CONFLICT, "Phone number already registered.");
        }
        if (detail.contains("uk_api_clients") || detail.contains("client_code") || detail.contains("api_key")) {
            return fail(HttpStatus.CONFLICT, "An API client with this value already exists.");
        }
        if (detail.contains("uk_contact") || detail.contains("uk_group")) {
            return fail(HttpStatus.CONFLICT, "This contact or group already exists.");
        }
        return fail(HttpStatus.CONFLICT, "A record with this value already exists.");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException ex) {
        log.error("Database error", ex);
        return fail(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save or load data. Please try again.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return fail(HttpStatus.INTERNAL_SERVER_ERROR, HumanReadableErrors.fromException(ex));
    }

    private String fieldMessage(FieldError error) {
        if (error.getDefaultMessage() != null && !error.getDefaultMessage().isBlank()) {
            return error.getDefaultMessage();
        }
        return error.getField() + " is invalid";
    }

    private String blankToGeneric(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }

    private ResponseEntity<ApiResponse<Void>> fail(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(message));
    }
}
