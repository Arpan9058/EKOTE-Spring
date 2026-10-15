package com.ekote.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class ValidationExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> invalidRequest(IllegalArgumentException exception) {
    return ResponseEntity.badRequest()
        .body(Map.of("status", "error", "errors", List.of(exception.getMessage())));
  }

  @ExceptionHandler(org.springframework.dao.DataAccessException.class)
  public ResponseEntity<Map<String, Object>> conflict(
      org.springframework.dao.DataAccessException exception) {
    org.slf4j.LoggerFactory.getLogger(ValidationExceptionHandler.class)
        .warn("Database operation rejected: {}", exception.getClass().getSimpleName());
    return ResponseEntity.status(409)
        .body(
            Map.of(
                "status",
                "error",
                "errors",
                List.of("Inventory changed or a referenced record exists; refresh and retry")));
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<Map<String, Object>> invalidParameters(
      HandlerMethodValidationException exception) {
    List<String> errors =
        exception.getParameterValidationResults().stream()
            .flatMap(result -> result.getResolvableErrors().stream())
            .map(error -> error.getDefaultMessage())
            .toList();
    return ResponseEntity.badRequest().body(Map.of("status", "error", "errors", errors));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> invalidForm(
      MethodArgumentNotValidException exception) {
    List<String> errors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList();
    return ResponseEntity.badRequest().body(Map.of("status", "error", "errors", errors));
  }
}
