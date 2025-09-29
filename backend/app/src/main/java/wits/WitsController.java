package wits;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "WITS Tariff Data", description = "Endpoints for retrieving tariff data from World Bank WITS")
public class WitsController {

    private final WitsApiService wits;

    public WitsController(WitsApiService wits) {
        this.wits = wits;
    }

    @Operation(
        summary = "Get demo tariff data (Full JSON)",
        description = "Returns the complete SDMX-JSON response for hardcoded example: US importing Rice from World, year 2020"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful retrieval of tariff data",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"structure\": {...}, \"dataSets\": [...]}")
            )
        )
    })
    @GetMapping("/api/wits/tariffs/demo")
    public ResponseEntity<String> tariffDemo() {
        return wits.getTariffData();
    }

    @Operation(
        summary = "Get demo MIN_RATE (Hardcoded)",
        description = "Returns only the MIN_RATE value from hardcoded example: US importing Rice from World, year 2020"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "MIN_RATE value retrieved successfully",
            content = @Content(examples = @ExampleObject(value = "0.0"))
        ),
        @ApiResponse(
            responseCode = "204",
            description = "No data found"
        )
    })
    @GetMapping("/api/wits/tariffs/demo/min-rate")
    public ResponseEntity<String> minRateDemo() {
        return wits.getMinRateOnly();
    }

    @Operation(
        summary = "Get MIN_RATE for specific parameters",
        description = "Returns the minimum tariff rate for a specific reporter-partner-product-year combination. " +
                      "Country codes can be either ISO codes or country names. " +
                      "Product codes should be HS6 codes (6-digit Harmonized System codes)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "MIN_RATE value or error message",
            content = @Content(
                examples = {
                    @ExampleObject(name = "Success", value = "2.5"),
                    @ExampleObject(name = "No Data", value = "No result found in WITS"),
                    @ExampleObject(name = "API Error", value = "API Error")
                }
            )
        )
    })
    @GetMapping("/api/wits/tariffs/min-rate")
    public ResponseEntity<String> getMinRate(
            @Parameter(
                description = "Reporting (Importing) country - use country name (e.g., 'China', 'USA') or ISO code (e.g., '156', '840')",
                example = "356",
                required = true
            ) 
            @RequestParam(defaultValue = "356") String reporter,

            @Parameter(
                description = "Partner (Exporting) country - use country name (e.g., 'USA', 'World') or ISO code (e.g., '840', '000'). Use 'World' or '000' for all partners",
                example = "000",
                required = true
            )
            @RequestParam(defaultValue = "000") String partner,
            
            @Parameter(
                description = "Product HS6 code (6-digit Harmonized System code). Example: '100610' for Rice (husked/brown)",
                example = "100630",
                required = true
            )
            @RequestParam(defaultValue = "100630") String product,
            
            @Parameter(
                description = "Year for tariff data (YYYY format)",
                example = "2020",
                required = true
            )
            @RequestParam(defaultValue = "2020") String year
    ) {
        return wits.getMinRateOnly(reporter, partner, product, year);
    }
}