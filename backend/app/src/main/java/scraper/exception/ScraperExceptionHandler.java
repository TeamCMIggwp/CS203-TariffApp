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

import scraper.dto.ErrorResponse;

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
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        log.warn("Validation error: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "Invalid input parameters",
            request.getDescription(false).replace("uri=", "")
        );
        
        // Add field-specific validation errors
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errorResponse.addValidationError(error.getField(), error.getDefaultMessage());
        }
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle search failures
     */
    @ExceptionHandler(SearchFailedException.class)
    public ResponseEntity<ErrorResponse> handleSearchFailedException(
            SearchFailedException ex,
            WebRequest request) {
        
        log.error("Search failed: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
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
    public ResponseEntity<ErrorResponse> handleScrapingFailedException(
            ScrapingFailedException ex,
            WebRequest request) {
        
        log.error("Scraping failed: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
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
    public ResponseEntity<ErrorResponse> handleScraperException(
            ScraperException ex,
            WebRequest request) {
        
        log.error("Scraper error: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Scraper Error",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}