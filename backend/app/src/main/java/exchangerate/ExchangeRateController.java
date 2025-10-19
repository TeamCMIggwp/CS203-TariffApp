package exchangerate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exchange")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @Autowired
    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    // Example: GET /api/v1/exchange?base=USD
    @GetMapping
    public ExchangeRateResponse getExchangeRates(@RequestParam String base) {
        return exchangeRateService.getExchangeRates(base);
    }
}
