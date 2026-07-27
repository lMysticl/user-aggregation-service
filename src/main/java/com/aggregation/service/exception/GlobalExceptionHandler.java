package com.aggregation.service.exception;

import com.aggregation.service.model.Error;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        Error error = Error.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Invalid request data")
                .details(errorMessage)
                .timestamp(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Error> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Relational data integrity violation: {}", ex.getClass().getSimpleName());
        Error error = Error.builder()
                .status(HttpStatus.CONFLICT.value())
                .message("Data integrity violation")
                .details("A user with this username already exists")
                .timestamp(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .build();

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(SourceUnavailableException.class)
    public ResponseEntity<Error> handleSourceUnavailable(SourceUnavailableException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error(
                "Data source failure [{}]: {} ({})",
                errorId,
                ex.getSource(),
                ex.getCause() == null
                        ? "unknown"
                        : ex.getCause().getClass().getSimpleName()
        );
        Error error = Error.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message("Data source unavailable")
                .details(ex.getSource() + " did not respond successfully; error ID: " + errorId)
                .timestamp(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .build();

        return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Error> handleGenericException(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        log.error(
                "Unhandled request failure [{}]: {}",
                errorId,
                ex.getClass().getSimpleName()
        );
        Error error = Error.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Internal server error")
                .details("Unexpected error; error ID: " + errorId)
                .timestamp(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .build();

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
