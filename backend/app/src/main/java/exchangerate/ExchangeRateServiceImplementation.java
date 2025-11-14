package exchangerate;

import java.util.*;
import java.util.stream.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.web.client.*;

@Service
public class ExchangeRateServiceImplementation implements ExchangeRateService {

    @Value("${exchange.api.key:dummy}")
    private String apiKey;

    @Value("${exchange.api.base-url:https://v6.exchangerate-api.com/v6}")
    private String exchangeApiBaseUrl;

    private static final String LATEST_PATH = "/latest/";

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ExchangeRateResponse getExchangeRates(String base) {
        String url = buildUrl(base);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        validateResponse(response);

        Map<String, Double> rates = parseRates(response);

        return new ExchangeRateResponse(
                (String) response.get("base_code"),
                rates
        );
    }

    private String buildUrl(String base) {
        return exchangeApiBaseUrl + "/" + apiKey + LATEST_PATH + base.toUpperCase();
    }

    private void validateResponse(Map<String, Object> response) {
        if (response == null || !"success".equals(response.get("result"))) {
            throw new RuntimeException("Failed to fetch exchange rates");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> parseRates(Map<String, Object> response) {
        Map<String, Object> raw = (Map<String, Object>) response.get("conversion_rates");
        return raw.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> ((Number) e.getValue()).doubleValue()
                ));
    }
}
