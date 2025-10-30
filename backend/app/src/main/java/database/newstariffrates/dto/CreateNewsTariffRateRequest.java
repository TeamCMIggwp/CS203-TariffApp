package database.newstariffrates.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateNewsTariffRateRequest {

    @NotBlank(message = "News link is required")
    @JsonProperty("newsLink")
    private String newsLink;

    @NotBlank(message = "Country ID is required")
    @Size(max = 3, message = "Country ID must be 3-character ISO code")
    @JsonProperty("countryId")
    private String countryId;

    @Size(max = 3, message = "Partner Country ID must be 3-character ISO code")
    @JsonProperty("partnerCountryId")
    private String partnerCountryId;

    @NotNull(message = "Product ID is required")
    @JsonProperty("productId")
    private Integer productId;

    @JsonProperty("tariffTypeId")
    private Integer tariffTypeId;

    @NotNull(message = "Year is required")
    @JsonProperty("year")
    private Integer year;

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.0", message = "Rate must be non-negative")
    @JsonProperty("rate")
    private Double rate;

    @JsonProperty("unit")
    private String unit = "%";

    // Getters and Setters
    public String getNewsLink() {
        return newsLink;
    }

    public void setNewsLink(String newsLink) {
        this.newsLink = newsLink;
    }

    public String getCountryId() {
        return countryId;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }

    public String getPartnerCountryId() {
        return partnerCountryId;
    }

    public void setPartnerCountryId(String partnerCountryId) {
        this.partnerCountryId = partnerCountryId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getTariffTypeId() {
        return tariffTypeId;
    }

    public void setTariffTypeId(Integer tariffTypeId) {
        this.tariffTypeId = tariffTypeId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return "CreateNewsTariffRateRequest{" +
                "newsLink='" + newsLink + '\'' +
                ", countryId='" + countryId + '\'' +
                ", partnerCountryId='" + partnerCountryId + '\'' +
                ", productId='" + productId + '\'' +
                ", tariffTypeId='" + tariffTypeId + '\'' +
                ", year=" + year +
                ", rate=" + rate +
                ", unit='" + unit + '\'' +
                '}';
    }
}
