package scraper.dto;

import jakarta.validation.constraints.*;

/**
 * Request DTO for creating a new scrape job
 */
public class ScrapeRequest {
    
    @NotBlank(message = "Query is required")
    @Size(min = 2, max = 200, message = "Query must be between 2 and 200 characters")
    private String query;
    
    @Min(value = 1, message = "Maximum results must be at least 1")
    @Max(value = 50, message = "Maximum results cannot exceed 50")
    private Integer maxResults = 10;
    
    @Min(value = 2000, message = "Minimum year must be at least 2000")
    @Max(value = 2030, message = "Minimum year cannot exceed 2030")
    private Integer minYear = 2020;
    
    // Constructors
    public ScrapeRequest() {}
    
    public ScrapeRequest(String query, Integer maxResults, Integer minYear) {
        this.query = query;
        this.maxResults = maxResults;
        this.minYear = minYear;
    }
    
    // Getters and setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public Integer getMaxResults() {
        return maxResults;
    }
    
    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }
    
    public Integer getMinYear() {
        return minYear;
    }
    
    public void setMinYear(Integer minYear) {
        this.minYear = minYear;
    }
}