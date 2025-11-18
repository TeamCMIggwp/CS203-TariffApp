package wits.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import common.exception.ApiErrorResponse;

import java.time.LocalDateTime;

@RestControllerAdvice
public class WitsExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(WitsExceptionHandler.class);
    
    @ExceptionHandler(WitsDataNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleWitsDataNotFound(
            WitsDataNotFoundException ex, WebRequest request) {
        logger.warn("WITS data not found: {}", ex.getMessage());

        ApiErrorResponse error = new ApiErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(WitsApiException.class)
    public ResponseEntity<ApiErrorResponse> handleWitsApiException(
            WitsApiException ex, WebRequest request) {
        logger.error("WITS API error: {}", ex.getMessage(), ex);

        ApiErrorResponse error = new ApiErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_GATEWAY.value(),
            "Bad Gateway",
            "Error communicating with WITS API: " + ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }
}