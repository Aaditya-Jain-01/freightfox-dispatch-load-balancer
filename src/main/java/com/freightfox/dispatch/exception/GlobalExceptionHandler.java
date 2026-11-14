package com.freightfox.dispatch.exception;

import com.freightfox.dispatch.dto.ErrorResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();

        ErrorResponse body = new ErrorResponse("error", "Validation failed for request payload", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableJson(HttpMessageNotReadableException ex) {
        ErrorResponse body = new ErrorResponse(
                "error",
                "Malformed JSON request or invalid enum value",
                List.of("Ensure valid JSON syntax and priority values (HIGH, MEDIUM, LOW)")
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(InvalidDispatchStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDispatchState(InvalidDispatchStateException ex) {
        ErrorResponse body = new ErrorResponse(
                "error",
                ex.getMessage(),
                List.of("Submit both delivery orders and fleet vehicles before requesting the dispatch plan")
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse body = new ErrorResponse(
                "error",
                "Unexpected internal server error",
                List.of(ex.getMessage() != null ? ex.getMessage() : "Unknown error")
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}