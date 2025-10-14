package wits.dto;

import jakarta.validation.constraints.*;

public class WitsTariffRateRequest {
    
    @NotBlank(message = "Reporter country is required")
    @Size(min = 3, max = 3, message = "Reporter must be 3-character ISO code")
    private String reporter;
    
    @NotBlank(message = "Partner country is required")
    @Size(min = 3, max = 3, message = "Partner must be 3-character ISO code")
    private String partner;
    
    @NotNull(message = "Product code is required")
    private Integer product;
    
    @NotBlank(message = "Year is required")
    @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits")
    private String year;
    
    // Constructors
    public WitsTariffRateRequest() {}
    
    public WitsTariffRateRequest(String reporter, String partner, Integer product, String year) {
        this.reporter = reporter;
        this.partner = partner;
        this.product = product;
        this.year = year;
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
}