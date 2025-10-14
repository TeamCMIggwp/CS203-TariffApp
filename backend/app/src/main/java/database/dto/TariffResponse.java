package database.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class TariffResponse {
    
    @JsonProperty("reporter")
    private String reporter;
    
    @JsonProperty("partner")
    private String partner;
    
    @JsonProperty("product")
    private Integer product;
    
    @JsonProperty("year")
    private String year;
    
    @JsonProperty("rate")
    private Double rate;
    
    @JsonProperty("unit")
    private String unit;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    public TariffResponse(String reporter, String partner, Integer product, 
                         String year, Double rate, String unit) {
        this.reporter = reporter;
        this.partner = partner;
        this.product = product;
        this.year = year;
        this.rate = rate;
        this.unit = unit;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }
    
    public String getPartner() { return partner; }
    public void setPartner(String partner) { this.partner = partner; }
    
    public Integer getProduct() { return product; }
    public void setProduct(Integer product) { this.product = product; }
    
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    
    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}