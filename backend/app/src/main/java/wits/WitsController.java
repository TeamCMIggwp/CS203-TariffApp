package wits;

import wits.dto.WitsTariffRateResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/wits")
@Tag(name = "WITS Tariff Data", description = "Access tariff data from World Bank WITS (World Integrated Trade Solution)")
@Validated
public class WitsController {
    private static final Logger logger = LoggerFactory.getLogger(WitsController.class);

    @Autowired
    private WitsApiService witsService;

    @Operation(
        summary = "Get tariff rate from WITS",
        description = """
            Retrieves tariff rate data from the World Bank WITS API for a specific 
            reporter-partner-product-year combination. Returns structured data including 
            minimum, maximum, and average tariff rates when available.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tariff data retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WitsTariffRateResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                          "reporter": "840",
                          "partner": "000",
                          "product": 100630,
                          "year": "2020",
                          "minRate": "0.0",
                          "maxRate": "5.0",
                          "avgRate": "2.5",
                          "source": "WITS",
                          "dataAvailable": true,
                          "timestamp": "2025-10-14T10:30:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No data found in WITS for the specified parameters",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "timestamp": "2025-10-14T10:30:00",
                          "status": 404,
                          "error": "Not Found",
                          "message": "No WITS data found for: reporter=840, partner=000, product=100630, year=2020",
                          "path": "/api/v1/wits/tariff-rates/840/000/100630/2020"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "502",
            description = "Error communicating with WITS API",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/tariff-rates/{reporter}/{partner}/{product}/{year}")
    public ResponseEntity<WitsTariffRateResponse> getTariffRate(
            @Parameter(
                description = "Reporting (importing) country - ISO 3-digit numeric code (e.g., '840' for USA, '356' for India)",
                example = "840",
                required = true
            )
            @PathVariable String reporter,

            @Parameter(
                description = "Partner (exporting) country - ISO 3-digit numeric code (e.g., '000' for World, '156' for China)",
                example = "000",
                required = true
            )
            @PathVariable String partner,
            
            @Parameter(
                description = "Product code - HS6 format (6-digit Harmonized System code, e.g., '100630' for Rice)",
                example = "100630",
                required = true
            )
            @PathVariable Integer product,
            
            @Parameter(
                description = "Year - 4-digit year (e.g., '2020')",
                example = "2020",
                required = true
            )
            @PathVariable String year
    ) {
        logger.info("GET /api/v1/wits/tariff-rates/{}/{}/{}/{}", reporter, partner, product, year);
        
        WitsTariffRateResponse response = witsService.getTariffRate(reporter, partner, product, year);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get raw WITS data (Advanced)",
        description = """
            Returns the complete raw SDMX-JSON response from the WITS API. 
            This endpoint is for advanced users who need access to all metadata 
            and attributes in the original WITS format.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Raw WITS data retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"structure\": {...}, \"dataSets\": [...]}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No data found in WITS"
        )
    })
    @GetMapping("/tariff-rates/{reporter}/{partner}/{product}/{year}/raw")
    public ResponseEntity<String> getRawTariffData(
            @PathVariable String reporter,
            @PathVariable String partner,
            @PathVariable Integer product,
            @PathVariable String year
    ) {
        logger.info("GET /api/v1/wits/tariff-rates/{}/{}/{}/{}/raw", reporter, partner, product, year);
        
        String rawData = witsService.getRawTariffData(reporter, partner, product, year);
        return ResponseEntity.ok(rawData);
    }
}