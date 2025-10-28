package scraper.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import scraper.dto.ScrapeRequest;
import scraper.dto.ScrapeResponse;
import scraper.service.ScraperService;

/**
 * RESTful controller for tariff scraping operations
 * 
 * Resource: Scrape Jobs
 * Base Path: /api/v1/scrape-jobs
 */
@RestController
@RequestMapping("/api/v1/scrape-jobs")
@CrossOrigin(origins = {"http://localhost:3000", "https://teamcmiggwp.duckdns.org"})
public class ScraperController {
    
    private static final Logger log = LoggerFactory.getLogger(ScraperController.class);
    
    @Autowired
    private ScraperService scraperService;
    
    /**
     * Create a new scrape job (synchronous execution for now)
     * 
     * POST /api/v1/scrape-jobs
     * 
     * Request Body:
     * {
     *   "query": "rice tariff",
     *   "maxResults": 10,
     *   "minYear": 2024
     * }
     * 
     * Response: 200 OK with ScrapeResponse
     */
    @PostMapping
    public ResponseEntity<ScrapeResponse> createScrapeJob(
            @Valid @RequestBody ScrapeRequest request) {
        
        log.info("Creating scrape job for query: {}", request.getQuery());
        
        ScrapeResponse response = scraperService.executeScrapeJob(request);
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
    
    /**
     * Health check endpoint
     * 
     * GET /api/v1/scrape-jobs/health
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse health = new HealthResponse("OK", "Scraper service is running");
        return ResponseEntity.ok(health);
    }
    
    /**
     * Simple health response DTO
     */
    public static class HealthResponse {
        private String status;
        private String message;
        
        public HealthResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
        
        public String getStatus() {
            return status;
        }
        
        public String getMessage() {
            return message;
        }
    }
}