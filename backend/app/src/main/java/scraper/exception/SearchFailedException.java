package scraper.exception;

/**
 * Exception thrown when search operation fails
 */
public class SearchFailedException extends ScraperException {
    
    public SearchFailedException(String message) {
        super(message);
    }
    
    public SearchFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}