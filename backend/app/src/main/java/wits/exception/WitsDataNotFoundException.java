package wits.exception;

public class WitsDataNotFoundException extends RuntimeException {
    private String reporter;
    private String partner;
    private Integer product;
    private String year;
    
    public WitsDataNotFoundException(String reporter, String partner, 
                                    Integer product, String year) {
        super(String.format(
            "No WITS data found for: reporter=%s, partner=%s, product=%s, year=%s",
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