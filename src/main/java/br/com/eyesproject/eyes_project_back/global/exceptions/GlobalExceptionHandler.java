package br.com.eyesproject.eyes_project_back.global.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INTERNAL_ERROR_DETAIL =
            "Não foi possível concluir a solicitação. Tente novamente mais tarde.";

    private final ApiProblemFactory problemFactory;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Recurso não encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "CONFLICT", "Conflito de estado", ex.getMessage(), request);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(DomainException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", "Regra de negócio inválida",
                ex.getMessage(), request);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ProblemDetail> handleTooManyRequests(
            TooManyRequestsException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.getRetryAfterSeconds()))
                .body(problemFactory.create(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                        "Limite de solicitações excedido", ex.getMessage(), request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMalformedBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Requisição inválida",
                "O corpo da requisição está ausente ou possui formato inválido.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        var validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> new ApiValidationError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        ProblemDetail problem = problemFactory.create(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED",
                "Falha de validação", "Um ou mais campos possuem valores inválidos.", request);
        problem.setProperty("errors", validationErrors);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        var validationErrors = ex.getConstraintViolations().stream()
                .map(violation -> new ApiValidationError(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .sorted(Comparator.comparing(ApiValidationError::field))
                .toList();
        ProblemDetail problem = problemFactory.create(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED",
                "Falha de validação", "Um ou mais parâmetros possuem valores inválidos.", request);
        problem.setProperty("errors", validationErrors);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        LOGGER.error("Unexpected request failure: method={}, path={}, correlationId={}",
                request.getMethod(), request.getRequestURI(), problemFactory.correlationId(request), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Erro interno",
                INTERNAL_ERROR_DETAIL, request);
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status,
            String code,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status).body(problemFactory.create(status, code, title, detail, request));
    }
}
