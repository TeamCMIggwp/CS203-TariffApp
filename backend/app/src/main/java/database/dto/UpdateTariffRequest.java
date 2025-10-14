package database.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateTariffRequest {
    
    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.0", message = "Rate must be non-negative")
    @JsonProperty("rate")
    private Double rate;
    
    @JsonProperty("unit")
    private String unit = "percent";
    
    // Getters and Setters
    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}