package exchangerate;

import org.springframework.beans.factory.annotation.*;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.*;

@RestController
@RequestMapping("/api/v1/exchange")
@Tag(name = "Exchange Rate", description = "RESTful API to query exchange rates")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @Autowired
    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    // Example: GET /api/v1/exchange?base=USD
    @Operation(
            summary = "Query exchange rate",
            description = "Query exchange rate with base country code."
    )
    @GetMapping
    public ExchangeRateResponse getExchangeRates(@RequestParam String base) {
        return exchangeRateService.getExchangeRates(base);
    }
}
