package wits.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WitsTariffRateResponse {
    
    @JsonProperty("reporter")
    private String reporter;
    
    @JsonProperty("partner")
    private String partner;
    
    @JsonProperty("product")
    private Integer product;
    
    @JsonProperty("year")
    private String year;
    
    @JsonProperty("minRate")
    private String minRate;
    
    @JsonProperty("maxRate")
    private String maxRate;
    
    @JsonProperty("avgRate")
    private String avgRate;
    
    @JsonProperty("source")
    private String source = "WITS";
    
    @JsonProperty("dataAvailable")
    private boolean dataAvailable;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    // THIS CONSTRUCTOR - Make sure it exists!
    public WitsTariffRateResponse(String reporter, String partner, Integer product, String year,
                                 String minRate, String maxRate, String avgRate) {
        this.reporter = reporter;
        this.partner = partner;
        this.product = product;
        this.year = year;
        this.minRate = minRate;
        this.maxRate = maxRate;
        this.avgRate = avgRate;
        this.dataAvailable = true;
        this.timestamp = LocalDateTime.now();
    }
    
    // Constructor for no data found
    public WitsTariffRateResponse(String reporter, String partner, Integer product, String year,
                                 String message) {
        this.reporter = reporter;
        this.partner = partner;
        this.product = product;
        this.year = year;
        this.dataAvailable = false;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    // All getters and setters below
    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }
    
    public String getPartner() { return partner; }
    public void setPartner(String partner) { this.partner = partner; }
    
    public Integer getProduct() { return product; }
    public void setProduct(Integer product) { this.product = product; }
    
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    
    public String getMinRate() { return minRate; }
    public void setMinRate(String minRate) { this.minRate = minRate; }
    
    public String getMaxRate() { return maxRate; }
    public void setMaxRate(String maxRate) { this.maxRate = maxRate; }
    
    public String getAvgRate() { return avgRate; }
    public void setAvgRate(String avgRate) { this.avgRate = avgRate; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public boolean isDataAvailable() { return dataAvailable; }
    public void setDataAvailable(boolean dataAvailable) { this.dataAvailable = dataAvailable; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}