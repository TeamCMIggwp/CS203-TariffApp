package scraper.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import scraper.dto.ScrapeRequest;
import scraper.dto.ScrapeResponse;
import scraper.dto.ScrapeResponse.ScrapedArticle;
import scraper.exception.ScraperException;
import scraper.model.SearchResult;
import scraper.model.ScrapedData;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Main service that orchestrates the scraping process
 * Follows Single Responsibility Principle
 */
@Service
public class ScraperService {
    
    private static final Logger log = LoggerFactory.getLogger(ScraperService.class);
    private static final long DELAY_BETWEEN_SCRAPES_MS = 2000;
    
    @Autowired
    private SearchService searchService;
    
    @Autowired
    private ContentScraperService contentScraperService;
    
    /**
     * Execute a complete scrape job
     */
    public ScrapeResponse executeScrapeJob(ScrapeRequest request) {
        Instant startTime = Instant.now();
        String jobId = UUID.randomUUID().toString();
        
        log.info("Starting scrape job {} for query: {}", jobId, request.getQuery());
        
        ScrapeResponse response = initializeResponse(jobId, request, startTime);
        
        try {
            // Step 1: Search for sources
            List<SearchResult> searchResults = searchService.search(
                request.getQuery(),
                request.getMaxResults()
            );
            
            response.setTotalSourcesFound(searchResults.size());
            response.setStatus(ScrapeResponse.JobStatus.IN_PROGRESS);
            
            log.info("Found {} sources for job {}", searchResults.size(), jobId);
            
            // Step 2: Scrape each source with rate limiting
            List<ScrapedArticle> articles = scrapeWithRateLimit(
                searchResults,
                request.getMinYear(),
                response,
                jobId
            );
            
            response.setArticles(articles);
            response.setSourcesScraped(articles.size());
            response.setStatus(ScrapeResponse.JobStatus.COMPLETED);
            
        } catch (Exception e) {
            log.error("Scrape job {} failed: {}", jobId, e.getMessage(), e);
            response.setStatus(ScrapeResponse.JobStatus.FAILED);
            response.getErrors().put("GENERAL", e.getMessage());
            throw new ScraperException("Scrape job failed", e);
        } finally {
            Instant endTime = Instant.now();
            response.setEndTime(endTime);
            response.getMeta().setDurationMs(Duration.between(startTime, endTime).toMillis());
            
            log.info("Scrape job {} completed. Scraped {}/{} sources in {}ms",
                    jobId,
                    response.getSourcesScraped(),
                    response.getTotalSourcesFound(),
                    response.getMeta().getDurationMs());
        }
        
        return response;
    }
    
    /**
     * Initialize response object
     */
    private ScrapeResponse initializeResponse(String jobId, ScrapeRequest request, Instant startTime) {
        ScrapeResponse response = new ScrapeResponse();
        response.setJobId(jobId);
        response.setQuery(request.getQuery());
        response.setStatus(ScrapeResponse.JobStatus.PENDING);
        response.setStartTime(startTime);
        response.getMeta().setMaxResults(request.getMaxResults());
        response.getMeta().setMinYear(request.getMinYear());
        return response;
    }
    
    /**
     * Scrape sources with rate limiting
     */
    private List<ScrapedArticle> scrapeWithRateLimit(
            List<SearchResult> searchResults,
            Integer minYear,
            ScrapeResponse response,
            String jobId) {
        
        List<ScrapedArticle> articles = new ArrayList<>();
        
        for (int i = 0; i < searchResults.size(); i++) {
            SearchResult searchResult = searchResults.get(i);
            
            log.debug("Scraping {}/{} for job {}: {}",
                    i + 1,
                    searchResults.size(),
                    jobId,
                    searchResult.getUrl());
            
            try {
                ScrapedData scrapedData = contentScraperService.scrape(
                    searchResult.getUrl(),
                    searchResult.getTitle()
                );
                
                if (scrapedData != null && isRecentEnough(scrapedData.getPublishDate(), minYear)) {
                    articles.add(mapToArticle(scrapedData));
                } else if (scrapedData != null) {
                    log.debug("Filtered out old article: {} ({})",
                            searchResult.getTitle(),
                            scrapedData.getPublishDate());
                }
                
                // Rate limiting: wait between scrapes (except for last one)
                if (i < searchResults.size() - 1) {
                    Thread.sleep(DELAY_BETWEEN_SCRAPES_MS);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Scraping interrupted for job {}", jobId);
                break;
            } catch (Exception e) {
                log.warn("Failed to scrape {} for job {}: {}",
                        searchResult.getUrl(),
                        jobId,
                        e.getMessage());
                response.getErrors().put(searchResult.getUrl(), e.getMessage());
            }
        }
        
        return articles;
    }
    
    /**
     * Check if article is recent enough based on minYear
     */
    private boolean isRecentEnough(String publishDate, Integer minYear) {
        if (publishDate == null || publishDate.isEmpty()) {
            return true; // Include if no date available
        }
        
        // Extract year from date string
        java.util.regex.Pattern yearPattern = java.util.regex.Pattern.compile("\\b(20\\d{2})\\b");
        java.util.regex.Matcher matcher = yearPattern.matcher(publishDate);
        
        if (matcher.find()) {
            try {
                int year = Integer.parseInt(matcher.group(1));
                return year >= minYear;
            } catch (NumberFormatException e) {
                return true;
            }
        }
        
        return true;
    }
    
    /**
     * Map internal model to DTO
     */
    private ScrapedArticle mapToArticle(ScrapedData data) {
        ScrapedArticle article = new ScrapedArticle();
        article.setUrl(data.getUrl());
        article.setTitle(data.getTitle());
        article.setSourceDomain(data.getSourceDomain());
        article.setRelevantText(data.getRelevantText());
        article.setExtractedRate(data.getExtractedRate());
        article.setPublishDate(data.getPublishDate());
        return article;
    }
}