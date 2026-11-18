package com.sakan.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global REST exception handler returning RFC 7807 ProblemDetail for all domain and framework exceptions.
 * SKN-13: Standardized error responses across the Sakan platform.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_ERROR_URI = "https://api.sakan.com/errors/";

    private ProblemDetail createProblem(HttpStatus status, String title, String detail, String errorSlug, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail != null ? detail : title);
        problem.setTitle(title);
        problem.setType(URI.create(BASE_ERROR_URI + errorSlug));
        problem.setProperty("timestamp", Instant.now());
        if (request != null) {
            problem.setInstance(URI.create(request.getRequestURI()));
        }
        return problem;
    }

    // 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), "bad-request", request);
    }

    @ExceptionHandler(RoleNotAllowedException.class)
    public ProblemDetail handleRoleNotAllowed(RoleNotAllowedException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), "role-not-allowed", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String msg = String.format("Parameter '%s' value '%s' could not be converted", ex.getName(), ex.getValue());
        return createProblem(HttpStatus.BAD_REQUEST, "Type Mismatch", msg, "type-mismatch", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed or invalid request body", "malformed-request", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        String summary = errors.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .sorted()
                .collect(Collectors.joining("; "));

        ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, "Validation Failed",
                summary.isEmpty() ? "Request validation failed" : summary, "validation", request);
        problem.setProperty("errors", errors);
        return problem;
    }

    // 401 Unauthorized
    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), "unauthorized", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.UNAUTHORIZED, "Unauthorized",
                ex.getMessage() != null ? ex.getMessage() : "Authentication required", "unauthorized", request);
    }

    // 403 Forbidden
    @ExceptionHandler(ForbiddenOperationException.class)
    public ProblemDetail handleForbidden(ForbiddenOperationException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), "forbidden", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.FORBIDDEN, "Forbidden",
                ex.getMessage() != null ? ex.getMessage() : "Access denied", "forbidden", request);
    }

    // 404 Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(), "not-found", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(), "not-found", request);
    }

    // 405 Method Not Allowed
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", ex.getMessage(), "method-not-allowed", request);
    }

    // 409 Conflict
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ProblemDetail handleInvalidStatusTransition(InvalidStatusTransitionException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), "conflict", request);
    }

    @ExceptionHandler(SlotConflictException.class)
    public ProblemDetail handleSlotConflict(SlotConflictException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), "slot-conflict", request);
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ProblemDetail handleDuplicateUsername(DuplicateUsernameException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), "duplicate-username", request);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), "duplicate-email", request);
    }

    // 415 Unsupported Media Type
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return createProblem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", ex.getMessage(), "unsupported-media-type", request);
    }

    // 500 Internal Server Error
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
        return createProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected internal server error occurred", "internal-server-error", request);
    }
}
