package tariffcalculator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple test controller for validating API connectivity
 */
@RestController
@RequestMapping("/tariff")
@Tag(name = "Testing", description = "Simple testing endpoints for API validation")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    /**
     * Test endpoint that takes 4 integer parameters and returns a fixed message
     */
    @GetMapping("/calculate")
    @Operation(
            summary = "Test trade data endpoint",
            description = "Takes 4 integer parameters (reportingCountry, partnerCountry, productCode, year) and returns a test message. " +
                         "Example: /tariff/calculate?reportingCountry=1&partnerCountry=2&productCode=3&year=2023"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Test message returned successfully"
    )
    public ResponseEntity<Map<String, Object>> getTradeData(
            @Parameter(
                    description = "Reporting country code (integer)",
                    example = "156",
                    required = true
            )
            @RequestParam Integer reportingCountry,
            
            @Parameter(
                    description = "Partner country code (integer)", 
                    example = "840",
                    required = true
            )
            @RequestParam Integer partnerCountry,
            
            @Parameter(
                    description = "Product code (integer)",
                    example = "123456",
                    required = true
            )
            @RequestParam Integer productCode,
            
            @Parameter(
                    description = "Year (integer)",
                    example = "2023",
                    required = true
            )
            @RequestParam Integer year) {

        logger.info("Test endpoint called with parameters: reportingCountry={}, partnerCountry={}, productCode={}, year={}", 
                    reportingCountry, partnerCountry, productCode, year);

        // Create response
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "i love don ta");
        response.put("timestamp", System.currentTimeMillis());
        response.put("parameters", Map.of(
            "reportingCountry", reportingCountry,
            "partnerCountry", partnerCountry, 
            "productCode", productCode,
            "year", year
        ));

        logger.info("Test endpoint response: {}", response.get("message"));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Simple health check for the test module
     */
    @GetMapping("/health")
    @Operation(
            summary = "Test module health check",
            description = "Returns health status of the testing module"
    )
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "testing-module");
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "Test module is working!");
        
        return ResponseEntity.ok(health);
    }
}