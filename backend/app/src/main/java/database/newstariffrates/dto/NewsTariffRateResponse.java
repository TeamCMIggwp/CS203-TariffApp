package database.newstariffrates.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class NewsTariffRateResponse {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("newsLink")
    private String newsLink;

    @JsonProperty("countryId")
    private String countryId;

    @JsonProperty("partnerCountryId")
    private String partnerCountryId;

    @JsonProperty("productId")
    private Integer productId;

    @JsonProperty("tariffTypeId")
    private Integer tariffTypeId;

    @JsonProperty("year")
    private Integer year;

    @JsonProperty("rate")
    private Double rate;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("lastUpdated")
    private java.sql.Timestamp lastUpdated;

    @JsonProperty("inEffect")
    private Boolean inEffect;

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public java.sql.Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(java.sql.Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Boolean getInEffect() {
        return inEffect;
    }

    public void setInEffect(Boolean inEffect) {
        this.inEffect = inEffect;
    }
}
