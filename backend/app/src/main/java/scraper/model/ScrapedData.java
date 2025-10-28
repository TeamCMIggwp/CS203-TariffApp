package scraper.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain model for scraped article data
 */
public class ScrapedData {
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
    
    public ScrapedData(String url, String title) {
        this.url = url;
        this.title = title;
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