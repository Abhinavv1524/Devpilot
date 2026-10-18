
package com.devpilot.orchestrator.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(TaskNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleTaskNotFound(
                        TaskNotFoundException exception) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidation(
                        MethodArgumentNotValidException exception) {

                String message = exception.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .findFirst()
                                .map(error -> error.getDefaultMessage())
                                .orElse("Invalid request");

                return ResponseEntity.badRequest()
                                .body(Map.of("error", message));
        }

        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<Map<String, String>> handleIllegalState(
                        IllegalStateException exception) {

                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", exception.getMessage()));
        }

}
