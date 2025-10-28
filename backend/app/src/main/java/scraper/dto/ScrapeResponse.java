package scraper.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for scrape job results
 */
public class ScrapeResponse {
    
    private String query;
    private JobStatus status;
    private Instant startTime;
    private Instant endTime;
    private Integer totalSourcesFound;
    private Integer sourcesScraped;
    private List<ScrapedArticle> articles;
    private Map<String, String> errors;
    private MetaData meta;
    
    public ScrapeResponse() {
        this.articles = new ArrayList<>();
        this.errors = new HashMap<>();
        this.meta = new MetaData();
    }
    
    // Getters and setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public JobStatus getStatus() {
        return status;
    }
    
    public void setStatus(JobStatus status) {
        this.status = status;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
    
    public Integer getTotalSourcesFound() {
        return totalSourcesFound;
    }
    
    public void setTotalSourcesFound(Integer totalSourcesFound) {
        this.totalSourcesFound = totalSourcesFound;
    }
    
    public Integer getSourcesScraped() {
        return sourcesScraped;
    }
    
    public void setSourcesScraped(Integer sourcesScraped) {
        this.sourcesScraped = sourcesScraped;
    }
    
    public List<ScrapedArticle> getArticles() {
        return articles;
    }
    
    public void setArticles(List<ScrapedArticle> articles) {
        this.articles = articles;
    }
    
    public Map<String, String> getErrors() {
        return errors;
    }
    
    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }
    
    public MetaData getMeta() {
        return meta;
    }
    
    public void setMeta(MetaData meta) {
        this.meta = meta;
    }
    
    /**
     * Nested class for scraped article data
     */
    public static class ScrapedArticle {
        private String url;
        private String title;
        private String sourceDomain;
        private List<String> relevantText;
        private String exporter;
        private String importer;
        private String product;
        private String year;
        private String tariffRate;
        private String publishDate;
        
        public ScrapedArticle() {
            this.relevantText = new ArrayList<>();
        }
        
        // Getters and setters
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getSourceDomain() {
            return sourceDomain;
        }
        
        public void setSourceDomain(String sourceDomain) {
            this.sourceDomain = sourceDomain;
        }
        
        public List<String> getRelevantText() {
            return relevantText;
        }
        
        public void setRelevantText(List<String> relevantText) {
            this.relevantText = relevantText;
        }

        public String getExporter() {
            return exporter;
        }

        public void setExporter(String exporter) {
            this.exporter = exporter;
        }

        public String getImporter() {
            return importer;
        }

        public void setImporter(String importer) {
            this.importer = importer;
        }

        public String getProduct() {
            return product;
        }

        public void setProduct(String product) {
            this.product = product;
        }

        public String getYear() {
            return year;
        }

        public void setYear(String year) {
            this.year = year;
        }

        public String getTariffRate() {
            return tariffRate;
        }

        public void setTariffRate(String tariffRate) {
            this.tariffRate = tariffRate;
        }

        public String getPublishDate() {
            return publishDate;
        }
        
        public void setPublishDate(String publishDate) {
            this.publishDate = publishDate;
        }
    }
    
    /**
     * Nested class for metadata
     */
    public static class MetaData {
        private Integer minYear;
        private Integer maxResults;

        // Getters and setters
        public Integer getMinYear() {
            return minYear;
        }

        public void setMinYear(Integer minYear) {
            this.minYear = minYear;
        }

        public Integer getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(Integer maxResults) {
            this.maxResults = maxResults;
        }
    }
    
    /**
     * Job status enum
     */
    public enum JobStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}