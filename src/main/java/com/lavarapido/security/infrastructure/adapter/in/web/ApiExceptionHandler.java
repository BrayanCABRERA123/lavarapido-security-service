package com.lavarapido.security.infrastructure.adapter.in.web;

import com.lavarapido.security.domain.exception.AccountDisabledException;
import com.lavarapido.security.domain.exception.CannotDisableOwnAccountException;
import com.lavarapido.security.domain.exception.DocumentAlreadyRegisteredException;
import com.lavarapido.security.domain.exception.DomainException;
import com.lavarapido.security.domain.exception.EmailAlreadyRegisteredException;
import com.lavarapido.security.domain.exception.InvalidCredentialsException;
import com.lavarapido.security.domain.exception.UserNotFoundException;
import com.lavarapido.security.domain.exception.WeakPasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Todo error sale como un cuerpo RFC 9457 {@code application/problem+json} con un
 * {@code code} estable que traduce el frontend. Los detalles internos nunca llegan al cliente.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException exception) {
        ProblemDetail problem = problem(statusOf(exception), exception.code(), exception.getMessage());
        if (exception instanceof WeakPasswordException weak) {
            problem.setProperty("violations", weak.violations());
        }
        return problem;
    }

    /** Una restricción única perdió la carrera contra una petición concurrente (ej. el mismo correo dos veces). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleConstraintViolation(DataIntegrityViolationException exception) {
        log.warn("Integrity constraint violated: {}", exception.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "CONFLICT", "The data conflicts with an existing record");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The request has invalid fields");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /** Los errores propios de Spring MVC (JSON mal formado, método incorrecto...) también llevan {@code code}. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode statusCode,
                                                          WebRequest request) {
        if (body instanceof ProblemDetail problem && problem.getProperties() == null) {
            problem.setProperty("code", HttpStatus.valueOf(statusCode.value()).name());
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private static HttpStatus statusOf(DomainException exception) {
        return switch (exception) {
            case InvalidCredentialsException ignored -> HttpStatus.UNAUTHORIZED;
            case AccountDisabledException ignored -> HttpStatus.FORBIDDEN;
            case CannotDisableOwnAccountException ignored -> HttpStatus.FORBIDDEN;
            case UserNotFoundException ignored -> HttpStatus.NOT_FOUND;
            case EmailAlreadyRegisteredException ignored -> HttpStatus.CONFLICT;
            case DocumentAlreadyRegisteredException ignored -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        return problem;
    }
}
