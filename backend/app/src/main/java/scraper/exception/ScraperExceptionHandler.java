package scraper.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import common.exception.ApiErrorResponse;
import common.exception.ValidationErrorResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception handler specifically for scraper-related errors
 * Uses @Order to ensure it handles scraper exceptions with lower precedence
 */
@RestControllerAdvice(basePackages = "scraper")
@Order(1)
public class ScraperExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ScraperExceptionHandler.class);
    
    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        log.warn("Validation error: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ValidationErrorResponse response = new ValidationErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            errors,
            request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Handle search failures
     */
    @ExceptionHandler(SearchFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleSearchFailedException(
            SearchFailedException ex,
            WebRequest request) {

        log.error("Search failed: {}", ex.getMessage());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_GATEWAY.value(),
            "Search Failed",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_GATEWAY);
    }
    
    /**
     * Handle scraping failures
     */
    @ExceptionHandler(ScrapingFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleScrapingFailedException(
            ScrapingFailedException ex,
            WebRequest request) {

        log.error("Scraping failed: {}", ex.getMessage());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Scraping Failed",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Handle generic scraper exceptions
     */
    @ExceptionHandler(ScraperException.class)
    public ResponseEntity<ApiErrorResponse> handleScraperException(
            ScraperException ex,
            WebRequest request) {

        log.error("Scraper error: {}", ex.getMessage());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Scraper Error",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}