package wits;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WitsController {

    private final WitsApiService wits;

    public WitsController(WitsApiService wits) {
        this.wits = wits;
    }

    // Hardcoded service method for testing (entire json response)
    @GetMapping("/api/wits/tariffs/demo")
    public ResponseEntity<String> tariffDemo() {
        return wits.getTariffData();
    }

    // Hardcoded demo returning MIN_RATE (single value)
    @GetMapping("/api/wits/tariffs/demo/min-rate")
    public ResponseEntity<String> minRateDemo() {
        return wits.getMinRateOnly();
    }

    // Parameterized MIN_RATE (takes in 4 parameters and return single value)
    @GetMapping("/api/wits/tariffs/min-rate")
    public ResponseEntity<String> getMinRate(
            @RequestParam String reporter,
            @RequestParam String partner,
            @RequestParam String product,
            @RequestParam String year

    ) {
        return wits.getMinRateOnly(reporter, partner, product, year);
    }

}