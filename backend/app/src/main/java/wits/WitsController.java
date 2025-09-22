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

    // Hardcoded service method for testing
    @GetMapping("/api/wits/tariffs/demo")
    public ResponseEntity<String> tariffDemo() {
        return wits.getTariffData();
    }

    // Parameterized version for production use
    @GetMapping("/api/wits/tariffs")
    public ResponseEntity<String> getTariffs(
        @RequestParam String reporter,
        @RequestParam String partner,
        @RequestParam String product,
        @RequestParam String year,
        @RequestParam(defaultValue = "reported") String datatype
    ) {
        return wits.getTariffData(reporter, partner, product, year, datatype);
    }
}