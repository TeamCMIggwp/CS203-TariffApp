package database.tariffs.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetTariffRequest {
    
    @Size(min = 3, max = 3, message = "Reporter must be 3-character ISO code")
    @JsonProperty("reporter")
    private String reporter;
    
    @Size(min = 3, max = 3, message = "Partner must be 3-character ISO code")
    @JsonProperty("partner")
    private String partner;
    
    @JsonProperty("product")
    private Integer product;
    
    @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits")
    @JsonProperty("year")
    private String year;
    
    // Getters and Setters
    public String getReporter() { 
        return reporter; 
    }
    
    public void setReporter(String reporter) { 
        this.reporter = reporter; 
    }
    
    public String getPartner() { 
        return partner; 
    }
    
    public void setPartner(String partner) { 
        this.partner = partner; 
    }
    
    public Integer getProduct() { 
        return product; 
    }
    
    public void setProduct(Integer product) { 
        this.product = product; 
    }
    
    public String getYear() { 
        return year; 
    }
    
    public void setYear(String year) { 
        this.year = year; 
    }
    
    @Override
    public String toString() {
        return "GetTariffRequest{" +
                "reporter='" + reporter + '\'' +
                ", partner='" + partner + '\'' +
                ", product=" + product +
                ", year='" + year + '\'' +
                '}';
    }
}