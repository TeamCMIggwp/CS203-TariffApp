package database.newstariffrates.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TariffRates")
public class NewsTariffRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tariff_id")
    private Integer id;

    @Column(name = "news_link", nullable = false, unique = true, length = 200)
    private String newsLink;

    @Column(name = "country_id", length = 3)
    private String countryId;

    @Column(name = "partner_country_id", length = 3)
    private String partnerCountryId;

    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "tariff_type_id")
    private Integer tariffTypeId;

    @Column(name = "year")
    private Integer year;

    @Column(name = "rate", precision = 6, scale = 3)
    private Double rate;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "last_updated")
    private java.sql.Timestamp lastUpdated;

    @Column(name = "in_effect")
    private Boolean inEffect;

    @PrePersist
    protected void onCreate() {
        lastUpdated = new java.sql.Timestamp(System.currentTimeMillis());
        if (inEffect == null) {
            inEffect = true; // Default to active
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = new java.sql.Timestamp(System.currentTimeMillis());
    }

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
