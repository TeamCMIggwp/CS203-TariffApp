package persistence;

public class TariffRate {
    private Integer partnerId;      // fromCountry id
    private Integer countryId;      // toCountry id
    private Integer productHsCode;  // 6-digit HS code
    private String year;
    private Double rate;

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
}
