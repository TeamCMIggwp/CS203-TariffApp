package database.tariffs.exception;

public class TariffNotFoundException extends RuntimeException {
    private String reporter;
    private String partner;
    private Integer product;
    private String year;
    
    public TariffNotFoundException(String reporter, String partner, 
                                   Integer product, String year) {
        super(String.format(
            "Tariff not found for: reporter=%s, partner=%s, product=%s, year=%s",
            reporter, partner, product, year
        ));
        this.reporter = reporter;
        this.partner = partner;
        this.product = product;
        this.year = year;
    }
    
    // Getters
    public String getReporter() { return reporter; }
    public String getPartner() { return partner; }
    public Integer getProduct() { return product; }
    public String getYear() { return year; }
}