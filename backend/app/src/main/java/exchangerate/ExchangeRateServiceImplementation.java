package exchangerate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ExchangeRateServiceImplementation implements ExchangeRateService {

    @Value("${exchange.api.key:dummy}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ExchangeRateResponse getExchangeRates(String base) {
        String url = "https://v6.exchangerate-api.com/v6/" + apiKey + "/latest/" + base.toUpperCase();

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response == null || !"success".equals(response.get("result"))) {
            throw new RuntimeException("Failed to fetch exchange rates");
        }

        @SuppressWarnings("unchecked")
        Map<String, Double> rates = (Map<String, Double>) response.get("conversion_rates");

        return new ExchangeRateResponse(
                (String) response.get("base_code"),
                rates
        );
    }
}