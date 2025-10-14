package scraper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/scraper")
@CrossOrigin(origins = {"http://localhost:3000", "https://teamcmiggwp.duckdns.org"})
public class ScraperController {
    
    @Autowired
    private TariffScraperService scraperService;
    
    /**
     * Main endpoint: Search official sources and scrape over time
     * 
     * POST /api/scraper/search-and-scrape
     * Body: {
     *   "query": "USA steel tariffs 2025",
     *   "maxResults": 10,
     *   "minYear": 2024
     * }
     */
    @PostMapping("/search-and-scrape")
    public ResponseEntity<?> searchAndScrape(@RequestBody Map<String, Object> request) {
        
        String query = (String) request.get("query");
        
        // Get maxResults with default of 10 if not provided
        Integer maxResults = request.containsKey("maxResults") 
            ? ((Number) request.get("maxResults")).intValue()
            : 10;
        
        // Get minYear with default of 2020 if not provided
        Integer minYear = request.containsKey("minYear") 
            ? ((Number) request.get("minYear")).intValue()
            : 2020;
        
        // Validate inputs
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Query is required"));
        }
        
        if (maxResults < 1 || maxResults > 50) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "maxResults must be between 1 and 50"));
        }
        
        if (minYear < 2000 || minYear > 2030) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "minYear must be between 2000 and 2030"));
        }
        
        try {
            // Execute async scraping
            CompletableFuture<TariffScraperService.ScrapeResult> future = 
                scraperService.searchAndScrape(query, maxResults, minYear);
            
            // Wait for completion (scrapes happen with delays over time)
            TariffScraperService.ScrapeResult result = future.get();
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Scraping failed: " + e.getMessage()));
        }
    }
}