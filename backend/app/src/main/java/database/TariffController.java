package database;

import database.dto.CreateTariffRequest;
import database.dto.UpdateTariffRequest;
import database.dto.TariffResponse;
import database.service.TariffService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/tariffs")
@CrossOrigin(origins = { "http://localhost:3000", "https://teamcmiggwp.duckdns.org" })
@Tag(name = "Tariff Management", description = "RESTful API for managing tariff rates")
@Validated
public class TariffController {
    private static final Logger logger = LoggerFactory.getLogger(TariffController.class);

    @Autowired
    private TariffService tariffService;

    @Operation(summary = "Create new tariff rate", description = "Creates a new tariff rate. Returns 409 Conflict if tariff already exists.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tariff created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TariffResponse.class), examples = @ExampleObject(value = "{\"reporter\": \"840\", \"partner\": \"356\", \"product\": 100630, \"year\": \"2020\", \"rate\": 24.0, \"unit\": \"percent\", \"timestamp\": \"2025-10-14T10:30:00\"}"))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Tariff already exists", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"timestamp\": \"2025-10-14T10:30:00\", \"status\": 409, \"error\": \"Conflict\", \"message\": \"Tariff already exists for: reporter=840, partner=356, product=100630, year=2020\", \"path\": \"/api/v1/tariffs\"}")))
    })
    @PostMapping
    public ResponseEntity<TariffResponse> createTariff(
            @Valid @RequestBody CreateTariffRequest request) {

        logger.info("POST /api/v1/tariffs - Creating tariff: {}", request);

        TariffResponse response = tariffService.createTariff(request);

        // Build Location header
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{reporter}/{partner}/{product}/{year}")
                .buildAndExpand(
                        response.getReporter(),
                        response.getPartner(),
                        response.getProduct(),
                        response.getYear())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Update existing tariff rate", description = "Updates an existing tariff rate. Returns 404 if tariff doesn't exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tariff updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TariffResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tariff not found", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"timestamp\": \"2025-10-14T10:30:00\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Tariff not found for: reporter=840, partner=356, product=100630, year=2020\", \"path\": \"/api/v1/tariffs/840/356/100630/2020\"}")))
    })
    @PutMapping("/{reporter}/{partner}/{product}/{year}")
    public ResponseEntity<TariffResponse> updateTariff(
            @Parameter(description = "Reporter country ISO code", example = "840") @PathVariable String reporter,

            @Parameter(description = "Partner country ISO code", example = "356") @PathVariable String partner,

            @Parameter(description = "Product HS code", example = "100630") @PathVariable Integer product,

            @Parameter(description = "Year", example = "2020") @PathVariable String year,

            @Valid @RequestBody UpdateTariffRequest request) {

        logger.info("PUT /api/v1/tariffs/{}/{}/{}/{} - Updating tariff",
                reporter, partner, product, year);

        TariffResponse response = tariffService.updateTariff(
                reporter, partner, product, year, request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get tariff rate", description = "Retrieves a specific tariff rate by composite key")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tariff found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TariffResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tariff not found")
    })
    // @GetMapping("/{reporter}/{partner}/{product}/{year}")
    // public ResponseEntity<TariffResponse> getTariff(
    // @PathVariable String reporter,
    // @PathVariable String partner,
    // @PathVariable Integer product,
    // @PathVariable String year) {

    // logger.info("GET /api/v1/tariffs/{}/{}/{}/{}", reporter, partner, product,
    // year);

    // TariffResponse response = tariffService.getTariff(reporter, partner, product,
    // year);
    // return ResponseEntity.ok(response);
    // }
    @GetMapping
    public ResponseEntity<TariffResponse> getTariff(
            @Parameter(description = "Reporting economy code, e.g., SGP", required = true) @RequestParam String reporter,
            @Parameter(description = "Partner economy code, e.g., CHN", required = true) @RequestParam String partner,
            @Parameter(description = "Product (HS) code, e.g., 1006", required = true) @RequestParam Integer product,
            @Parameter(description = "Year of tariff data, e.g., 2023", required = true) @RequestParam String year) {

        logger.info("GET /api/v1/tariffs?reporter={}&partner={}&product={}&year={}",
                reporter, partner, product, year);

        TariffResponse response = tariffService.getTariff(reporter, partner, product, year);
        return ResponseEntity.ok(response);
    }
}