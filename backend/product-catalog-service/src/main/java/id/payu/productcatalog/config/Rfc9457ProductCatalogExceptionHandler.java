package id.payu.productcatalog.config;

import id.payu.api.common.exception.problem.Rfc9457GlobalExceptionHandler;
import id.payu.productcatalog.application.service.ProductNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@org.springframework.core.annotation.Order(0)
public class Rfc9457ProductCatalogExceptionHandler extends Rfc9457GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleProductNotFound(
            ProductNotFoundException ex, HttpServletRequest request) {
        log.warn("Product not found: {}", ex.getProductCode());
        return respondWith(HttpStatus.NOT_FOUND, "Resource not found",
                ex.getMessage(), "PRODUCT_NOT_FOUND", request);
    }

    @Override
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", errors);
        return respondWith(HttpStatus.BAD_REQUEST, "Bad request", errors, "VALIDATION_ERROR", request);
    }

    @Override
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<id.payu.api.common.exception.problem.ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return respondWith(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage(), "VALIDATION_ERROR", request);
    }
}
