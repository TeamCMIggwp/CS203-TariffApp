package exchangerate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Simple DTO test to cover constructor and getters.
 */
class ExchangeRateResponseTest {

    @Test
    void getters_returnValues() {
        Map<String, Double> rates = Map.of("SGD", 1.35, "MYR", 3.10);
        ExchangeRateResponse r = new ExchangeRateResponse("USD", rates);

        assertEquals("USD", r.getBaseCurrency());
        assertEquals(2, r.getConversionRates().size());
        assertEquals(1.35, r.getConversionRates().get("SGD"));
        assertEquals(3.10, r.getConversionRates().get("MYR"));
    }
}
