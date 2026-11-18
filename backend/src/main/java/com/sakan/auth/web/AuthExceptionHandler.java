package com.sakan.auth.web;

import com.sakan.common.exception.DuplicateEmailException;
import com.sakan.common.exception.DuplicateUsernameException;
import com.sakan.common.exception.InvalidCredentialsException;
import com.sakan.common.exception.RoleNotAllowedException;
import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// TEMP-STUB SKN-13: scoped to AuthController so it does not clash with M6's global handler.
// Fold into the global handler when SKN-13 is merged.
@RestControllerAdvice(assignableTypes = AuthController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionHandler {

    @ExceptionHandler(DuplicateUsernameException.class)
    public ProblemDetail duplicateUsername(DuplicateUsernameException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail duplicateEmail(DuplicateEmailException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(RoleNotAllowedException.class)
    public ProblemDetail roleNotAllowed(RoleNotAllowedException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail invalidCredentials(InvalidCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    /** Field names and messages only: the rejected value (maybe a password) is never echoed. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Validation Failed", detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadable(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed or invalid request body");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}