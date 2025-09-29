package database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/database")
@CrossOrigin(origins = {"http://localhost:3000", "https://teamcmiggwp.duckdns.org"})
public class TariffController {
    private static final Logger logger = LoggerFactory.getLogger(TariffController.class);

    @Autowired
    private TariffRateRepository tariffRepository;

    @PostMapping("/update")
    public ResponseEntity<?> updateTariffRate(@RequestBody TariffRateEntity tariffRate) {
        logger.info("Received request to update tariff rate: {}", tariffRate);

        try {
            // Validate Strings for iso codes as non-empty
            if (tariffRate.getPartnerIsoNumeric() == null || tariffRate.getPartnerIsoNumeric().trim().isEmpty() ||
                tariffRate.getCountryIsoNumeric() == null || tariffRate.getCountryIsoNumeric().trim().isEmpty() ||
                tariffRate.getProductHsCode() == null || tariffRate.getYear() == null || tariffRate.getYear().trim().isEmpty() ||
                tariffRate.getRate() == null) {
                logger.warn("Validation failed: missing required fields in tariffRate: {}", tariffRate);
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("All fields are required"));
            }

            tariffRepository.updateTariffRate(tariffRate);

            logger.info("Tariff rate updated successfully for: {}", tariffRate);
            return ResponseEntity.ok(new SuccessResponse("Tariff rate updated successfully"));

        } catch (Exception e) {
            logger.error("Failed to update tariff rate for {}: {}", tariffRate, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Failed to update tariff rate: " + e.getMessage()));
        }
    }

    // Retrieve accepts reporter and partner as String iso codes (char(3)) and product as int
    @GetMapping("/retrieve")
    public ResponseEntity<?> retrieveTariffRate(
            @RequestParam String reporter,
            @RequestParam String partner,
            @RequestParam Integer product,
            @RequestParam String year) {

        logger.info("Received request to retrieve tariff rate: reporter={}, partner={}, product={}, year={}",
                   reporter, partner, product, year);

        try {
            // Validate required parameters
            if (reporter == null || reporter.trim().isEmpty() ||
                partner == null || partner.trim().isEmpty() ||
                product == null || year == null || year.trim().isEmpty()) {
                logger.warn("Validation failed: missing required parameters");
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("All parameters (reporter, partner, product, year) are required"));
            }

            // Retrieve tariff rate from database (note: now repository expects iso codes directly)
            Double tariffRate = tariffRepository.getTariffRate(reporter, partner, product, year);

            if (tariffRate == null) {
                logger.info("No tariff rate found for: reporter={}, partner={}, product={}, year={}",
                           reporter, partner, product, year);
                return ResponseEntity.notFound().build();
            }

            logger.info("Successfully retrieved tariff rate: {} for reporter={}, partner={}, product={}, year={}",
                       tariffRate, reporter, partner, product, year);

            return ResponseEntity.ok(new TariffRateResponse(tariffRate));

        } catch (Exception e) {
            logger.error("Failed to retrieve tariff rate for reporter={}, partner={}, product={}, year={}: {}",
                        reporter, partner, product, year, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Failed to retrieve tariff rate: " + e.getMessage()));
        }
    }

    private static class ErrorResponse {
        private final String message;
        public ErrorResponse(String message) { this.message = message; }

        @JsonProperty("message")
        public String getMessage() { return message; }
    }

    private static class SuccessResponse {
        private final String message;
        public SuccessResponse(String message) { this.message = message; }

        @JsonProperty("message")
        public String getMessage() { return message; }
    }

    private static class TariffRateResponse {
        private final Double rate;
        public TariffRateResponse(Double rate) { this.rate = rate; }

        @JsonProperty("rate")
        public Double getRate() { return rate; }
    }
}
