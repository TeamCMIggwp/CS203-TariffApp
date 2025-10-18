package database.tariffs.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateTariffRequest {
    
    @NotBlank(message = "Reporter country is required")
    @Size(min = 3, max = 3, message = "Reporter must be 3-character ISO code")
    @JsonProperty("reporter")
    private String reporter;
    
    @NotBlank(message = "Partner country is required")
    @Size(min = 3, max = 3, message = "Partner must be 3-character ISO code")
    @JsonProperty("partner")
    private String partner;
    
    @NotNull(message = "Product code is required")
    @JsonProperty("product")
    private Integer product;
    
    @NotBlank(message = "Year is required")
    @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits")
    @JsonProperty("year")
    private String year;
    
    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.0", message = "Rate must be non-negative")
    @JsonProperty("rate")
    private Double rate;
    
    @JsonProperty("unit")
    private String unit = "percent";
    
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
}