package exchangerate;

public interface ExchangeRateService {
    ExchangeRateResponse getExchangeRates(String base);
}