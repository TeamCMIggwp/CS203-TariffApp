package wits.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@RestControllerAdvice
public class WitsExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(WitsExceptionHandler.class);
    
    @ExceptionHandler(WitsDataNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWitsDataNotFound(
            WitsDataNotFoundException ex, WebRequest request) {
        logger.warn("WITS data not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(WitsApiException.class)
    public ResponseEntity<ErrorResponse> handleWitsApiException(
            WitsApiException ex, WebRequest request) {
        logger.error("WITS API error: {}", ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_GATEWAY.value(),
            "Bad Gateway",
            "Error communicating with WITS API: " + ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }
    
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        
        public ErrorResponse(LocalDateTime timestamp, int status, String error, 
                           String message, String path) {
            this.timestamp = timestamp;
            this.status = status;
            this.error = error;
            this.message = message;
            this.path = path;
        }
        
        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public int getStatus() { return status; }
        public String getError() { return error; }
        public String getMessage() { return message; }
        public String getPath() { return path; }
    }
}