package database;

public class TariffRateEntity {
    private Integer partnerId;      // DB ID after resolving from iso_numeric
    private Integer countryId;      // DB ID after resolving from iso_numeric
    private Integer productHsCode;  // 6-digit HS code from frontend (still int)
    private String year;
    private Double rate;

    private int countryIsoNumeric;    // iso_numeric from frontend (string converted to int)
    private int partnerIsoNumeric;    // iso_numeric from frontend

    // Getters and setters

    public Integer getPartnerId() { return partnerId; }
    public void setPartnerId(Integer partnerId) { this.partnerId = partnerId; }
    
    public Integer getCountryId() { return countryId; }
    public void setCountryId(Integer countryId) { this.countryId = countryId; }
    
    public Integer getProductHsCode() { return productHsCode; }
    public void setProductHsCode(Integer productHsCode) { this.productHsCode = productHsCode; }
    
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    
    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }

    public int getCountryIsoNumeric() {
        return countryIsoNumeric;
    }

    public void setCountryIsoNumeric(int countryIsoNumeric) {
        this.countryIsoNumeric = countryIsoNumeric;
    }

    public int getPartnerIsoNumeric() {
        return partnerIsoNumeric;
    }

    public void setPartnerIsoNumeric(int partnerIsoNumeric) {
        this.partnerIsoNumeric = partnerIsoNumeric;
    }

    @Override
    public String toString() {
        return "TariffRate{" +
            "partnerId=" + partnerId +
            ", countryId=" + countryId +
            ", productHsCode=" + productHsCode +
            ", year='" + year + '\'' +
            ", rate=" + rate +
            ", partnerIsoNumeric=" + partnerIsoNumeric +
            ", countryIsoNumeric=" + countryIsoNumeric +
            '}';
    }
}

