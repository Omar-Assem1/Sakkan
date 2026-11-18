package com.sakan.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    @DisplayName("IllegalArgumentException returns 400 with ProblemDetail")
    void handleIllegalArgument() {
        ProblemDetail problem = handler.handleIllegalArgument(new IllegalArgumentException("Invalid input"), request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getDetail()).isEqualTo("Invalid input");
        assertThat(problem.getType()).isEqualTo(URI.create("https://api.sakan.com/errors/bad-request"));
        assertThat(problem.getInstance()).isEqualTo(URI.create("/api/test"));
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("RoleNotAllowedException returns 400 with ProblemDetail")
    void handleRoleNotAllowed() {
        ProblemDetail problem = handler.handleRoleNotAllowed(new RoleNotAllowedException(), request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getType()).isEqualTo(URI.create("https://api.sakan.com/errors/role-not-allowed"));
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException returns 400 with parameter details")
    void handleTypeMismatch() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "id", null, null);

        ProblemDetail problem = handler.handleTypeMismatch(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Type Mismatch");
        assertThat(problem.getDetail()).contains("Parameter 'id' value 'abc' could not be converted");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException returns 400 Bad Request")
    void handleNotReadable() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("unreadable", (org.springframework.http.HttpInputMessage) null);

        ProblemDetail problem = handler.handleNotReadable(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getDetail()).isEqualTo("Malformed or invalid request body");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException returns 400 with field errors")
    void handleValidation() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("userDto", "email", "Email must be valid");
        when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));

        MethodParameter parameter = mock(MethodParameter.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ProblemDetail problem = handler.handleValidation(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Validation Failed");
        assertThat(problem.getDetail()).contains("email: Email must be valid");
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
        assertThat(errors).containsEntry("email", "Email must be valid");
    }

    @Test
    @DisplayName("InvalidCredentialsException returns 401 Unauthorized")
    void handleInvalidCredentials() {
        ProblemDetail problem = handler.handleInvalidCredentials(new InvalidCredentialsException(), request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problem.getTitle()).isEqualTo("Unauthorized");
        assertThat(problem.getDetail()).isEqualTo("Invalid username or password");
    }

    @Test
    @DisplayName("AuthenticationException returns 401 Unauthorized")
    void handleAuthentication() {
        AuthenticationException ex = new AuthenticationException("Bad token") {};

        ProblemDetail problem = handler.handleAuthentication(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problem.getTitle()).isEqualTo("Unauthorized");
    }

    @Test
    @DisplayName("ForbiddenOperationException returns 403 Forbidden")
    void handleForbidden() {
        ProblemDetail problem = handler.handleForbidden(new ForbiddenOperationException("Not the owner"), request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problem.getTitle()).isEqualTo("Forbidden");
        assertThat(problem.getDetail()).isEqualTo("Not the owner");
    }

    @Test
    @DisplayName("AccessDeniedException returns 403 Forbidden")
    void handleAccessDenied() {
        ProblemDetail problem = handler.handleAccessDenied(new AccessDeniedException("Access is denied"), request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problem.getTitle()).isEqualTo("Forbidden");
    }

    @Test
    @DisplayName("ResourceNotFoundException returns 404 Not Found")
    void handleNotFound() {
        ProblemDetail problem = handler.handleNotFound(new ResourceNotFoundException("Property not found"), request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problem.getDetail()).isEqualTo("Property not found");
    }

    @Test
    @DisplayName("NoResourceFoundException returns 404 Not Found")
    void handleNoResourceFound() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        when(ex.getMessage()).thenReturn("Path not found");

        ProblemDetail problem = handler.handleNoResourceFound(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException returns 405 Method Not Allowed")
    void handleMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");

        ProblemDetail problem = handler.handleMethodNotSupported(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
        assertThat(problem.getTitle()).isEqualTo("Method Not Allowed");
    }

    @Test
    @DisplayName("InvalidStatusTransitionException returns 409 Conflict")
    void handleInvalidStatusTransition() {
        ProblemDetail problem = handler.handleInvalidStatusTransition(
                new InvalidStatusTransitionException("Cannot move from ARCHIVED to PUBLISHED"), request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Conflict");
    }

    @Test
    @DisplayName("SlotConflictException returns 409 Conflict")
    void handleSlotConflict() {
        ProblemDetail problem = handler.handleSlotConflict(new SlotConflictException("Overlapping slot"), request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Conflict");
    }

    @Test
    @DisplayName("DuplicateUsernameException returns 409 Conflict")
    void handleDuplicateUsername() {
        ProblemDetail problem = handler.handleDuplicateUsername(new DuplicateUsernameException(), request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Conflict");
    }

    @Test
    @DisplayName("DuplicateEmailException returns 409 Conflict")
    void handleDuplicateEmail() {
        ProblemDetail problem = handler.handleDuplicateEmail(new DuplicateEmailException(), request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Conflict");
    }

    @Test
    @DisplayName("HttpMediaTypeNotSupportedException returns 415 Unsupported Media Type")
    void handleMediaTypeNotSupported() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException("Unsupported type");

        ProblemDetail problem = handler.handleMediaTypeNotSupported(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
        assertThat(problem.getTitle()).isEqualTo("Unsupported Media Type");
    }

    @Test
    @DisplayName("Exception fallback returns 500 Internal Server Error with safe message")
    void handleGenericException() {
        ProblemDetail problem = handler.handleGenericException(new RuntimeException("Database down"), request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).isEqualTo("An unexpected internal server error occurred");
        assertThat(problem.getType()).isEqualTo(URI.create("https://api.sakan.com/errors/internal-server-error"));
    }
}
