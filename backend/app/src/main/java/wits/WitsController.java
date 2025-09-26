package wits;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;

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
            @Parameter(
                    description = "Reporting (Importing) country code (string)",
                    example = "China",
                    required = true
            ) 
            @RequestParam String reporter,

            @Parameter(
                    description = "Partner (Exporting) country code (string)",
                    example = "USA",
                    required = true
            )
            @RequestParam String partner,
            
            @Parameter(
                    description = "Product code (integer)",
                    example = "100610",
                    required = true
            )
            @RequestParam String product,
            
            @Parameter(
                    description = "Year (integer)",
                    example = "2020",
                    required = true
            )
            @RequestParam String year

    ) {
        return wits.getMinRateOnly(reporter, partner, product, year);
    }

}