package database;

public class TariffRateEntity {

    private String countryIsoNumeric;
    private String partnerIsoNumeric;

    private Integer productHsCode;

    private String year;
    private Double rate;

    // Getters and setters
    
    public String getCountryIsoNumeric() {
        return countryIsoNumeric;
    }

    public void setCountryIsoNumeric(String countryIsoNumeric) {
        this.countryIsoNumeric = countryIsoNumeric;
    }

    public String getPartnerIsoNumeric() {
        return partnerIsoNumeric;
    }

    public void setPartnerIsoNumeric(String partnerIsoNumeric) {
        this.partnerIsoNumeric = partnerIsoNumeric;
    }

    public Integer getProductHsCode() { return productHsCode; }
    public void setProductHsCode(Integer productHsCode) { this.productHsCode = productHsCode; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }

    @Override
    public String toString() {
        return "TariffRateEntity{" +
                "countryIsoNumeric='" + countryIsoNumeric + '\'' +
                ", partnerIsoNumeric='" + partnerIsoNumeric + '\'' +
                ", productHsCode=" + productHsCode +
                ", year='" + year + '\'' +
                ", rate=" + rate +
                '}';
    }
}

