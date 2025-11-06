package exchangerate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

class ExchangeRateServiceImplementationTest {

    private ExchangeRateServiceImplementation service;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ExchangeRateServiceImplementation();

        // Inject apiKey and RestTemplate (private fields) to avoid real HTTP calls
        ReflectionTestUtils.setField(service, "apiKey", "testkey");
        // Even though the field is private final, Spring's ReflectionTestUtils can set it in tests
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("Success: returns parsed ExchangeRateResponse with mixed numeric types")
    void success_shouldReturnResponse() {
        String base = "usd";
        String expectedUrl = "https://v6.exchangerate-api.com/v6/testkey/latest/USD";

        Map<String, Object> conversion = new HashMap<>();
        conversion.put("SGD", 1.35);      // Double
        conversion.put("JPY", 150);       // Integer -> Number path
        conversion.put("EUR", 0.92D);     // Double

        Map<String, Object> payload = new HashMap<>();
        payload.put("result", "success");
        payload.put("base_code", "USD");
        payload.put("conversion_rates", conversion);

        when(restTemplate.getForObject(eq(expectedUrl), eq(Map.class))).thenReturn(payload);

        ExchangeRateResponse resp = service.getExchangeRates(base);

        assertNotNull(resp);
        assertEquals("USD", resp.getBaseCurrency());
        assertEquals(3, resp.getConversionRates().size());
        assertEquals(1.35, resp.getConversionRates().get("SGD"));
        assertEquals(150.0, resp.getConversionRates().get("JPY")); // cast to double
        assertEquals(0.92, resp.getConversionRates().get("EUR"));

        verify(restTemplate, times(1)).getForObject(eq(expectedUrl), eq(Map.class));
    }

    @Test
    @DisplayName("Failure: null response -> throws RuntimeException")
    void failure_nullResponse_throws() {
        String expectedUrl = "https://v6.exchangerate-api.com/v6/testkey/latest/GBP";
        when(restTemplate.getForObject(eq(expectedUrl), eq(Map.class))).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getExchangeRates("gbp"));
        assertTrue(ex.getMessage().toLowerCase().contains("failed"), "Should indicate fetch failure");
    }

    @Test
    @DisplayName("Failure: non-success 'result' -> throws RuntimeException")
    void failure_nonSuccessResult_throws() {
        String expectedUrl = "https://v6.exchangerate-api.com/v6/testkey/latest/JPY";

        Map<String, Object> payload = new HashMap<>();
        payload.put("result", "error"); // not "success"

        when(restTemplate.getForObject(eq(expectedUrl), eq(Map.class))).thenReturn(payload);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getExchangeRates("jpy"));
        assertTrue(ex.getMessage().toLowerCase().contains("failed"), "Should indicate fetch failure");
    }
}
