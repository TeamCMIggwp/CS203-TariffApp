package database.newstariffrates.entity;

public class NewsTariffRate {

    private Integer tariffId;
    private String newsLink;
    private String countryId;
    private String partnerCountryId;
    private Integer productId;
    private Integer tariffTypeId;
    private Integer year;
    private Double rate;
    private String unit;
    private java.sql.Timestamp lastUpdated;
    private Boolean inEffect;

    // Getters and Setters
    public Integer getTariffId() {
        return tariffId;
    }

    public void setTariffId(Integer tariffId) {
        this.tariffId = tariffId;
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
