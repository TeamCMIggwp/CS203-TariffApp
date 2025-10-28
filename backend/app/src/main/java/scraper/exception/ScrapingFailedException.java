package scraper.exception;

/**
 * Exception thrown when scraping operation fails
 */
public class ScrapingFailedException extends ScraperException {
    
    public ScrapingFailedException(String message) {
        super(message);
    }
    
    public ScrapingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}