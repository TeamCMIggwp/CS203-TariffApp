package database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import wits.WitsApiService; // Add this import

@RestController
@RequestMapping("/api/database")
@CrossOrigin(origins = {"http://localhost:3000", "https://teamcmiggwp.duckdns.org"})
@Tag(name = "Database Tariff Management", description = "Endpoints for managing tariff rates in the local database")
public class TariffController {
    private static final Logger logger = LoggerFactory.getLogger(TariffController.class);

    @Autowired
    private TariffRateRepository tariffRepository;

    @Autowired  // Add this
    private WitsApiService witsApiService;

    @PostMapping("/update")
    public ResponseEntity<?> updateTariffRate(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Tariff rate data to update or insert",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = TariffRateEntity.class),
                    examples = @ExampleObject(
                        name = "Example Tariff Rate",
                        value = "{\n" +
                                "  \"countryIsoNumeric\": \"840\",\n" +
                                "  \"partnerIsoNumeric\": \"356\",\n" +
                                "  \"productHsCode\": 100630,\n" +
                                "  \"year\": \"2020\",\n" +
                                "  \"rate\": 24,\n" +
                                "  \"unit\": \"percent\"\n" +
                                "}"
                    )
                )
            )
            @RequestBody TariffRateEntity tariffRate) {
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
    @Operation(
        summary = "Retrieve tariff rate",
        description = "Retrieves the tariff rate for a specific reporter-partner-product-year combination. " +
                      "First queries WITS API, then falls back to local database if no data found in WITS. " +
                      "Returns the tariff rate if found, or 404 if no matching record exists in either source."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tariff rate found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TariffRateResponse.class),
                examples = @ExampleObject(value = "{\"rate\": 24.0, \"source\": \"WITS\"}")
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - missing required parameters",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"message\": \"All parameters (reporter, partner, product, year) are required\"}")
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No tariff rate found in WITS or database"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = "{\"message\": \"Failed to retrieve tariff rate: ...\"}")
            )
        )
    })
    @GetMapping("/retrieve")
public ResponseEntity<?> retrieveTariffRate(
        @Parameter(
            description = "Reporter (Importing) country ISO numeric code (3 characters)",
            example = "840",
            required = true
        )
        @RequestParam(defaultValue = "840") String reporter,
        
        @Parameter(
            description = "Partner (Exporting) country ISO numeric code (3 characters)",
            example = "356",
            required = true
        )
        @RequestParam(defaultValue = "356") String partner,
        
        @Parameter(
            description = "Product HS code (integer)",
            example = "100630",
            required = true
        )
        @RequestParam(defaultValue = "100630") Integer product,
        
        @Parameter(
            description = "Year for tariff data (YYYY format)",
            example = "2020",
            required = true
        )
        @RequestParam(defaultValue = "2020") String year) {

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

        // STEP 1: Try to get data from WITS API first
        logger.info("Querying WITS API for tariff rate...");
        ResponseEntity<String> witsResponse = witsApiService.getMinRateOnly(
            reporter, partner, product.toString(), year
        );

        String witsData = witsResponse.getBody();
        logger.info("WITS API response: {}", witsData);

        // Check if WITS returned valid data (not an error or "no result" message)
        if (witsData != null && 
            !witsData.equalsIgnoreCase("No result found in WITS") && 
            !witsData.equalsIgnoreCase("API Error")) {
            
            try {
                // Try to parse the rate from WITS
                Double witsRate = Double.parseDouble(witsData.trim());
                logger.info("Successfully retrieved tariff rate from WITS: {}", witsRate);
                return ResponseEntity.ok(new TariffRateResponse(witsRate, "WITS"));
            } catch (NumberFormatException e) {
                logger.warn("WITS returned non-numeric data: {}", witsData);
                // Continue to database fallback
            }
        }

        // STEP 2: WITS has no data, fall back to local database
        logger.info("WITS has no data, querying local database...");
        Double dbRate = tariffRepository.getTariffRate(reporter, partner, product, year);

        if (dbRate == null) {
            logger.info("No tariff rate found in either WITS or database for: reporter={}, partner={}, product={}, year={}",
                       reporter, partner, product, year);
            return ResponseEntity.status(404)
                .body(new ErrorResponse(
                    "No tariff rate data found for the specified parameters. " +
                    "Neither the WITS API nor the local database contain information for " +
                    "reporter: " + reporter + ", partner: " + partner + 
                    ", product: " + product + ", year: " + year
                ));
        }

        logger.info("Successfully retrieved tariff rate from database: {} for reporter={}, partner={}, product={}, year={}",
                   dbRate, reporter, partner, product, year);

        return ResponseEntity.ok(new TariffRateResponse(dbRate, "Database"));

    } catch (Exception e) {
        logger.error("Failed to retrieve tariff rate for reporter={}, partner={}, product={}, year={}: {}",
                    reporter, partner, product, year, e.getMessage(), e);
        return ResponseEntity.internalServerError()
            .body(new ErrorResponse("Failed to retrieve tariff rate: " + e.getMessage()));
    }
}

    @Schema(description = "Error response")
    private static class ErrorResponse {
        private final String message;
        
        public ErrorResponse(String message) { 
            this.message = message; 
        }

        @Schema(description = "Error message", example = "All fields are required")
        @JsonProperty("message")
        public String getMessage() { 
            return message; 
        }
    }

    @Schema(description = "Success response")
    private static class SuccessResponse {
        private final String message;
        
        public SuccessResponse(String message) { 
            this.message = message; 
        }

        @Schema(description = "Success message", example = "Tariff rate updated successfully")
        @JsonProperty("message")
        public String getMessage() { 
            return message; 
        }
    }

    @Schema(description = "Tariff rate response")
    private static class TariffRateResponse {
        private final Double rate;
        private final String source;
        
        public TariffRateResponse(Double rate, String source) { 
            this.rate = rate;
            this.source = source;
        }

        @Schema(description = "Tariff rate value", example = "24.0")
        @JsonProperty("rate")
        public Double getRate() { 
            return rate; 
        }

        @Schema(description = "Data source (WITS or Database)", example = "WITS")
        @JsonProperty("source")
        public String getSource() {
            return source;
        }
    }
}