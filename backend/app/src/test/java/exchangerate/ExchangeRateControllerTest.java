package exchangerate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure unit tests for ExchangeRateController.
 * No Spring context, no MockMvc — mirrors ScraperControllerTest style.
 */
@ExtendWith(MockitoExtension.class)
class ExchangeRateControllerTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private ExchangeRateController controller;

    private ExchangeRateResponse usdResponse;

    @BeforeEach
    void setUp() {
        usdResponse = new ExchangeRateResponse(
                "USD",
                Map.of("SGD", 1.35, "EUR", 0.92)
        );
    }

    @Test
    void getExchangeRates_returnsResponseFromService() {
        when(exchangeRateService.getExchangeRates(eq("USD"))).thenReturn(usdResponse);

        ExchangeRateResponse resp = controller.getExchangeRates("USD");

        assertNotNull(resp);
        assertEquals("USD", resp.getBaseCurrency());
        assertEquals(1.35, resp.getConversionRates().get("SGD"));
        assertEquals(0.92, resp.getConversionRates().get("EUR"));

        verify(exchangeRateService, times(1)).getExchangeRates(eq("USD"));
        verifyNoMoreInteractions(exchangeRateService);
    }

    @Test
    void getExchangeRates_passesBaseThroughAsIs() {
        when(exchangeRateService.getExchangeRates(eq("jpy"))).thenReturn(
                new ExchangeRateResponse("JPY", Map.of("SGD", 0.01))
        );

        ExchangeRateResponse resp = controller.getExchangeRates("jpy");

        assertEquals("JPY", resp.getBaseCurrency());
        assertEquals(0.01, resp.getConversionRates().get("SGD"));
        verify(exchangeRateService).getExchangeRates(eq("jpy"));
    }
}
