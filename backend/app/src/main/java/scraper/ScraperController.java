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
     *   "maxResults": 5
     * }
     * 
     * This endpoint will:
     * 1. Search Google for official sources
     * 2. Scrape each source with 2-second delays between requests
     * 3. Return all extracted information
     */
    @PostMapping("/search-and-scrape")
    public ResponseEntity<?> searchAndScrape(@RequestBody Map<String, Object> request) {
        
        String query = (String) request.get("query");
        int maxResults = request.containsKey("maxResults") 
            ? (Integer) request.get("maxResults") 
            : 5;
        
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Query is required"));
        }
        
        try {
            // Execute async scraping
            CompletableFuture<TariffScraperService.ScrapeResult> future = 
                scraperService.searchAndScrape(query, maxResults);
            
            // Wait for completion (scrapes happen with delays over time)
            TariffScraperService.ScrapeResult result = future.get();
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Scraping failed: " + e.getMessage()));
        }
    }
}