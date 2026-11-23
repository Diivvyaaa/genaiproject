package com.codecrafthub.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides centralized error handling for the REST API.
 *
 * Instead of returning default Spring error messages, this class returns
 * simple and consistent JSON responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors caused by @Valid.
     *
     * Examples:
     * - Missing name
     * - Empty description
     * - Missing target_date
     * - Missing status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError :
                exception.getBindingResult().getFieldErrors()) {

            String fieldName = fieldError.getField();

            /*
             * The Java field is targetDate, but the JSON field is target_date.
             * Returning target_date makes the error clearer to API clients.
             */
            if ("targetDate".equals(fieldName)) {
                fieldName = "target_date";
            }

            fieldErrors.put(
                    fieldName,
                    fieldError.getDefaultMessage()
            );
        }

        Map<String, Object> response = createErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed"
        );

        response.put("errors", fieldErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handles invalid JSON, invalid dates, and invalid status values.
     *
     * For example, these values are invalid:
     *
     * "status": "Started"
     * "target_date": "15-12-2026"
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableMessage(
            HttpMessageNotReadableException exception
    ) {
        String message = "Request body contains invalid JSON";

        Throwable cause = exception.getMostSpecificCause();

        if (cause instanceof InvalidFormatException invalidFormatException) {
            String fieldName = invalidFormatException
                    .getPath()
                    .stream()
                    .findFirst()
                    .map(reference -> reference.getFieldName())
                    .orElse("unknown field");

            /*
             * Invalid status values are rejected by CourseStatus.fromValue().
             */
            if ("status".equals(fieldName)) {
                message = "status must be exactly one of: "
                        + "\"Not Started\", \"In Progress\", \"Completed\"";
            }

            /*
             * LocalDate parsing errors are handled here.
             */
            else if ("target_date".equals(fieldName)
                    || "targetDate".equals(fieldName)) {
                message = "target_date must use the format YYYY-MM-DD";
            }

            else {
                message = "Invalid value for field: " + fieldName;
            }
        }

        Map<String, Object> response = createErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handles a course lookup when the requested ID does not exist.
     */
    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCourseNotFound(
            CourseNotFoundException exception
    ) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * Handles errors involving courses.json.
     */
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Map<String, Object>> handleFileStorageException(
            FileStorageException exception
    ) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * Handles invalid path-variable types.
     *
     * Example:
     * GET /api/courses/abc
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Course ID must be a number"
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handles unexpected errors not covered by the specific handlers above.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception exception
    ) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected server error occurred"
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * Creates a consistent JSON error response.
     */
    private Map<String, Object> createErrorResponse(
            int status,
            String message
    ) {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("timestamp", Instant.now());
        response.put("status", status);
        response.put("message", message);

        return response;
    }
}
