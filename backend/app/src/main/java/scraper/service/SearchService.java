package scraper.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import scraper.exception.SearchFailedException;
import scraper.model.SearchResult;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service responsible for searching external sources
 * Handles Google Custom Search API integration
 */
@Service
public class SearchService {
    
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    
    @Value("${google.search.api.key:}")
    private String googleApiKey;
    
    @Value("${google.search.engine.id:}")
    private String searchEngineId;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    // Search query variations to get better results
    private static final String[] QUERY_VARIATIONS = {
        "%s tariff rate",
        "%s import duty",
        "%s customs tariff",
        "%s trade tariff"
    };
    
    public SearchService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Search for sources using multiple query variations
     */
    public List<SearchResult> search(String baseQuery, int maxResults) {
        validateConfiguration();
        
        log.info("Searching for: {}", baseQuery);
        
        Set<String> seenUrls = new HashSet<>();
        List<SearchResult> allResults = new ArrayList<>();
        
        // Try different query variations
        for (String variation : QUERY_VARIATIONS) {
            if (allResults.size() >= maxResults) {
                break;
            }
            
            String query = String.format(variation, baseQuery);
            List<SearchResult> batchResults = executeSearch(query, maxResults - allResults.size());
            
            // Deduplicate
            for (SearchResult result : batchResults) {
                if (!seenUrls.contains(result.getUrl()) && allResults.size() < maxResults) {
                    allResults.add(result);
                    seenUrls.add(result.getUrl());
                }
            }
        }
        
        log.info("Search completed. Found {} unique results", allResults.size());
        return allResults;
    }
    
    /**
     * Execute a single Google Custom Search API call
     */
    private List<SearchResult> executeSearch(String query, int maxResults) {
        try {
            String url = buildSearchUrl(query, Math.min(maxResults, 10));
            
            log.debug("Executing search: {}", query);
            
            String responseJson = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return parseSearchResponse(responseJson);
            
        } catch (Exception e) {
            log.error("Search API call failed: {}", e.getMessage());
            throw new SearchFailedException("Failed to execute search", e);
        }
    }
    
    /**
     * Build Google Custom Search URL
     */
    private String buildSearchUrl(String query, int num) {
        return String.format(
            "https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&num=%d&dateRestrict=y1",
            googleApiKey,
            searchEngineId,
            URLEncoder.encode(query, StandardCharsets.UTF_8),
            num
        );
    }
    
    /**
     * Parse Google Search JSON response
     */
    private List<SearchResult> parseSearchResponse(String json) {
        List<SearchResult> results = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(json);
            
            if (!root.has("items")) {
                log.warn("No items found in search response");
                
                if (root.has("error")) {
                    JsonNode error = root.get("error");
                    String errorMessage = error.has("message") 
                        ? error.get("message").asText() 
                        : "Unknown error";
                    throw new SearchFailedException("Google API error: " + errorMessage);
                }
                
                return results;
            }
            
            JsonNode items = root.get("items");
            
            for (JsonNode item : items) {
                String url = item.has("link") ? item.get("link").asText() : null;
                String title = item.has("title") ? item.get("title").asText() : null;
                
                if (url != null && title != null) {
                    results.add(new SearchResult(url, title));
                }
            }
            
            log.debug("Parsed {} search results", results.size());
            
        } catch (SearchFailedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse search response: {}", e.getMessage());
            throw new SearchFailedException("Failed to parse search results", e);
        }
        
        return results;
    }
    
    /**
     * Validate that Google Search API is configured
     */
    private void validateConfiguration() {
        if (googleApiKey == null || googleApiKey.isEmpty() ||
            searchEngineId == null || searchEngineId.isEmpty()) {
            throw new SearchFailedException("Google Search API is not configured. " +
                "Please set google.search.api.key and google.search.engine.id");
        }
    }
}