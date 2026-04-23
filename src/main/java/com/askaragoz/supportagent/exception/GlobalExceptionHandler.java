package com.askaragoz.supportagent.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

/**
 * Global exception handler — converts exceptions into RFC 7807 ProblemDetail responses.
 *
 * RFC 7807 is a standard format for HTTP error responses:
 *   {
 *     "type": "about:blank",
 *     "title": "Not Found",
 *     "status": 404,
 *     "detail": "Ticket not found: abc123",
 *     "instance": "/api/v1/tickets/abc123"
 *   }
 *
 * @RestControllerAdvice — applies this handler globally to all @RestController classes.
 *   Intercepting exceptions here means controllers stay clean — they never write
 *   error-handling code directly.
 *
 * extends ResponseEntityExceptionHandler — inherits Spring MVC's built-in handlers for
 *   standard exceptions. Combined with spring.mvc.problemdetails.enabled=true in
 *   application.yml, these standard handlers already return ProblemDetail for:
 *     - MethodArgumentNotValidException  (400 — validation failures with field details)
 *     - ResponseStatusException          (the status code thrown in services)
 *     - NoResourceFoundException         (404 — unknown URL path)
 *     - HttpMessageNotReadableException  (400 — malformed JSON body)
 *
 * This class only needs to handle exceptions NOT covered by the parent class.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Catch-all for any exception not handled by a more specific handler above.
     * Logs the full stack trace for debugging and returns a safe 500 response
     * that does NOT expose internal error details to the caller.
     *
     * ProblemDetail.forStatusAndDetail() — Spring Boot 3 factory method that
     * creates a ProblemDetail with the given HTTP status and human-readable detail.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
        problem.setType(URI.create("about:blank"));
        return problem;
    }
}
