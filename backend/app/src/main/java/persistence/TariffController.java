package persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:3000", "https://teamcmiggwp.duckdns.org"})
public class TariffController {
    private static final Logger logger = LoggerFactory.getLogger(TariffController.class);

    @Autowired
    private TariffRepository tariffRepository;

    @PostMapping("/update-tariff")
    public ResponseEntity<?> updateTariffRate(@RequestBody TariffRate tariffRate) {
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
}
