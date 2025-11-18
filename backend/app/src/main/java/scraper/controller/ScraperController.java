package scraper.controller;

import jakarta.validation.constraints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import scraper.dto.ScrapeRequest;
import scraper.dto.ScrapeResponse;
import scraper.service.ScraperService;

/**
 * RESTful controller for tariff scraping operations
 * 
 * Resource: Scrape
 * Base Path: /api/v1/scrape
 */
@RestController
@RequestMapping("/api/v1/scrape")
@CrossOrigin(origins = {"http://localhost:3000", "https://teamcmiggwp.duckdns.org"})
public class ScraperController {

    private static final Logger log = LoggerFactory.getLogger(ScraperController.class);

    private final ScraperService scraperService;

    public ScraperController(ScraperService scraperService) {
        this.scraperService = scraperService;
    }
    
    /**
     * Execute a scrape job with query parameters
     * 
     * GET /api/v1/scrape?query=rice%20tariff&maxResults=10&minYear=2024
     * 
     * Query Parameters:
     * - query: Search query (required, 2-200 characters)
     * - maxResults: Maximum number of results (optional, default: 10, range: 1-50)
     * - minYear: Minimum year for filtering (optional, default: 2020, range: 2000-2030)
     * 
     * Response: 200 OK with ScrapeResponse
     */
    @GetMapping
    public ResponseEntity<ScrapeResponse> executeScrape(
            @RequestParam 
            @NotBlank(message = "Query is required")
            @Size(min = 2, max = 200, message = "Query must be between 2 and 200 characters")
            String query,
            
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Maximum results must be at least 1")
            @Max(value = 50, message = "Maximum results cannot exceed 50")
            Integer maxResults,
            
            @RequestParam(defaultValue = "2020")
            @Min(value = 2000, message = "Minimum year must be at least 2000")
            @Max(value = 2030, message = "Minimum year cannot exceed 2030")
            Integer minYear) {
        
        log.info("Executing scrape for query: {}", query);

        ScrapeRequest request = new ScrapeRequest(query, maxResults, minYear);
        ScrapeResponse response = scraperService.executeScrapeJob(request);

        return ResponseEntity.ok(response);
    }
}