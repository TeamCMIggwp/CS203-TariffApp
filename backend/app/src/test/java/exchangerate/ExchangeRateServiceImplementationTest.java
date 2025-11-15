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

/**
 * Unit tests for ExchangeRateServiceImplementation.
 * No Spring context — we inject the private fields via ReflectionTestUtils.
 */
class ExchangeRateServiceImplementationTest {

    private ExchangeRateServiceImplementation service;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ExchangeRateServiceImplementation();

        // Inject apiKey, base URL, and RestTemplate (private fields) to avoid real HTTP calls
        ReflectionTestUtils.setField(service, "apiKey", "testkey");
        ReflectionTestUtils.setField(service, "exchangeApiBaseUrl", "https://v6.exchangerate-api.com/v6");
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("Success: parses response and converts Numbers to doubles")
    void success_shouldReturnResponse() {
        // Given a lowercase base, code should uppercase in URL
        String expectedUrl = "https://v6.exchangerate-api.com/v6/testkey/latest/USD";

        Map<String, Object> conversion = new HashMap<>();
        conversion.put("SGD", 1.35);     // Double
        conversion.put("JPY", 150);      // Integer, should convert to 150.0
        conversion.put("EUR", 0.92D);    // Double

        Map<String, Object> payload = new HashMap<>();
        payload.put("result", "success");
        payload.put("base_code", "USD");
        payload.put("conversion_rates", conversion);

        when(restTemplate.getForObject(eq(expectedUrl), eq(Map.class))).thenReturn(payload);

        ExchangeRateResponse resp = service.getExchangeRates("usd");

        assertNotNull(resp);
        assertEquals("USD", resp.getBaseCurrency());
        assertEquals(3, resp.getConversionRates().size());
        assertEquals(1.35, resp.getConversionRates().get("SGD"));
        assertEquals(150.0, resp.getConversionRates().get("JPY"));
        assertEquals(0.92, resp.getConversionRates().get("EUR"));

        verify(restTemplate, times(1)).getForObject(eq(expectedUrl), eq(Map.class));
        verifyNoMoreInteractions(restTemplate);
    }

    @Test
    @DisplayName("Failure: null HTTP response -> throws RuntimeException")
    void failure_nullResponse() {
        String expectedUrl = "https://v6.exchangerate-api.com/v6/testkey/latest/GBP";
        when(restTemplate.getForObject(eq(expectedUrl), eq(Map.class))).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getExchangeRates("gbp"));
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }

    @Test
    @DisplayName("Failure: result != success -> throws RuntimeException")
    void failure_nonSuccessResult() {
        String expectedUrl = "https://v6.exchangerate-api.com/v6/testkey/latest/JPY";

        Map<String, Object> payload = new HashMap<>();
        payload.put("result", "error");

        when(restTemplate.getForObject(eq(expectedUrl), eq(Map.class))).thenReturn(payload);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getExchangeRates("jpy"));
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }
}
