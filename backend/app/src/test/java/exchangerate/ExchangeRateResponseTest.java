package exchangerate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExchangeRateResponseTest {

    @Test
    @DisplayName("POJO getters return constructor values")
    void getters() {
        Map<String, Double> rates = Map.of("SGD", 1.35, "MYR", 4.3);
        ExchangeRateResponse r = new ExchangeRateResponse("USD", rates);

        assertEquals("USD", r.getBaseCurrency());
        assertEquals(2, r.getConversionRates().size());
        assertEquals(1.35, r.getConversionRates().get("SGD"));
        assertEquals(4.3, r.getConversionRates().get("MYR"));
    }
}
