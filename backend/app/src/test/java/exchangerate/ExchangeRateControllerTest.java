package exchangerate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ExchangeRateController.class)
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("GET /api/v1/exchange returns 200 with baseCurrency and conversionRates")
    void getExchangeRates_returnsOk() throws Exception {
        ExchangeRateResponse mocked = new ExchangeRateResponse(
                "USD",
                Map.of("SGD", 1.35, "EUR", 0.92)
        );

        when(exchangeRateService.getExchangeRates(eq("USD"))).thenReturn(mocked);

        mockMvc.perform(get("/api/v1/exchange")
                        .param("base", "USD")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.conversionRates.SGD").value(1.35))
                .andExpect(jsonPath("$.conversionRates.EUR").value(0.92));

        verify(exchangeRateService, times(1)).getExchangeRates(eq("USD"));
    }
}
