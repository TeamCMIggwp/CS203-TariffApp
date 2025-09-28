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
            // Validate that all fields exist
            if (tariffRate.getPartnerIsoNumeric() == 0 || tariffRate.getCountryIsoNumeric() == 0 ||
                tariffRate.getProductHsCode() == null || tariffRate.getYear() == null || tariffRate.getRate() == null) {
                logger.warn("Validation failed: missing required fields in tariffRate: {}", tariffRate);
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("All fields are required"));
            }

            // Resolve iso_numeric to internal IDs
            Integer countryId = tariffRepository.getCountryIdByIsoNumeric(tariffRate.getCountryIsoNumeric());
            Integer partnerId = tariffRepository.getCountryIdByIsoNumeric(tariffRate.getPartnerIsoNumeric());

            if (countryId == null || partnerId == null) {
                logger.warn("Invalid country ISO numeric codes: countryIsoNumeric={}, partnerIsoNumeric={}",
                    tariffRate.getCountryIsoNumeric(), tariffRate.getPartnerIsoNumeric());
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid country ISO numeric codes"));
            }

            // Set resolved IDs back to TariffRate for repository use
            tariffRate.setCountryId(countryId);
            tariffRate.setPartnerId(partnerId);

            tariffRepository.updateTariffRate(tariffRate);

            logger.info("Tariff rate updated successfully for: {}", tariffRate);
            return ResponseEntity.ok(new SuccessResponse("Tariff rate updated successfully"));

        } catch (Exception e) {
            logger.error("Failed to update tariff rate for {}: {}", tariffRate, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Failed to update tariff rate: " + e.getMessage()));
        }
    }

    @GetMapping("/retrieve")
public ResponseEntity<?> retrieveTariffRate(
        @RequestParam int reporter,
        @RequestParam int partner, 
        @RequestParam int product,
        @RequestParam String year) {
    
    logger.info("Received request to retrieve tariff rate: reporter={}, partner={}, product={}, year={}", 
               reporter, partner, product, year);

    try {
        // Validate required parameters
        if (reporter == 0 || partner == 0 || product == 0 || year == null || year.trim().isEmpty()) {
            logger.warn("Validation failed: missing required parameters");
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("All parameters (reporter, partner, product, year) are required"));
        }

        // Resolve ISO numeric codes to internal IDs
        Integer reporterCountryId = tariffRepository.getCountryIdByIsoNumeric(reporter);
        Integer partnerCountryId = tariffRepository.getCountryIdByIsoNumeric(partner);
        Integer productId = tariffRepository.getProductIdByHsCode(product);

        if (reporterCountryId == null) {
            logger.warn("Invalid reporter country ISO numeric code: {}", reporter);
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Invalid reporter country ISO numeric code: " + reporter));
        }

        if (partnerCountryId == null) {
            logger.warn("Invalid partner country ISO numeric code: {}", partner);
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Invalid partner country ISO numeric code: " + partner));
        }

        if (productId == null) {
            logger.warn("Invalid product HS code: {}", product);
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Invalid product HS code: " + product));
        }

        // Retrieve tariff rate from database
        Double tariffRate = tariffRepository.getTariffRate(reporterCountryId, partnerCountryId, productId, year);

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