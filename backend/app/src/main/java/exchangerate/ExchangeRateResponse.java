package exchangerate;

import java.util.Map;

public class ExchangeRateResponse {
    private String baseCurrency;
    private Map<String, BigDecimal> conversionRates;

    public ExchangeRateResponse(String baseCurrency, Map<String, Double> conversionRates) {
        this.baseCurrency = baseCurrency;
        this.conversionRates = conversionRates;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public Map<String, Double> getConversionRates() {
        return conversionRates;
    }
}

