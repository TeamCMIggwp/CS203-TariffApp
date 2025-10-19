package exchangerate;

import org.springframework.stereotype.Service;

@Service
public interface ExchangeRateService {
    ExchangeRateResponse getExchangeRates(String base);
}