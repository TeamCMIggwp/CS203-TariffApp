package database.tariffs.controller;

import database.tariffs.dto.CreateTariffRequest;
import database.tariffs.dto.UpdateTariffRequest;
import database.tariffs.dto.TariffResponse;
import database.tariffs.service.TariffService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

@RestController
@RequestMapping("/api/v1/database/tariffs")
@Tag(name = "Tariff Management", description = "RESTful API for managing tariff rates")
@Validated
public class TariffController {
    private static final Logger logger = LoggerFactory.getLogger(TariffController.class);

    @Autowired
    private TariffService tariffService;

    @Operation(
        summary = "Create new tariff rate",
        description = "Creates a new tariff rate. Returns 409 Conflict if tariff already exists."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Tariff created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TariffResponse.class),
                examples = @ExampleObject(
                    value = "{\"reporter\": \"840\", \"partner\": \"356\", \"product\": 100630, \"year\": \"2020\", \"rate\": 24.0, \"unit\": \"percent\", \"timestamp\": \"2025-10-14T10:30:00\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Tariff already exists",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"timestamp\": \"2025-10-14T10:30:00\", \"status\": 409, \"error\": \"Conflict\", \"message\": \"Tariff already exists for: reporter=840, partner=356, product=100630, year=2020\", \"path\": \"/api/v1/database/tariffs\"}"
                )
            )
        )
    })
    @PostMapping
    public ResponseEntity<TariffResponse> createTariff(@Valid @RequestBody CreateTariffRequest request) {
        
        logger.info("POST /api/v1/database/tariffs - Creating tariff: {}", request);

        TariffResponse response = tariffService.createTariff(request);

        return ResponseEntity.status(201).body(response);
    }

    @Operation(
        summary = "Update existing tariff rate",
        description = "Updates an existing tariff rate. Returns 404 if tariff doesn't exist."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tariff updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TariffResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Tariff not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"timestamp\": \"2025-10-14T10:30:00\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Tariff not found for: reporter=840, partner=356, product=100630, year=2020\", \"path\": \"/api/v1/database/tariffs\"}"
                )
            )
        )
    })
    @PutMapping
    public ResponseEntity<TariffResponse> updateTariff(@Valid @RequestBody UpdateTariffRequest request) {
        
        logger.info("PUT /api/v1/database/tariffs - Updating tariff: {}", request);

        TariffResponse response = tariffService.updateTariff(
                request.getReporter(),
                request.getPartner(),
                request.getProduct(),
                request.getYear(),
                request);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get specific tariff rate",
        description = "Get specific tariff by providing reporter, partner, product, and year as query parameters. All parameters are required."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tariff retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"reporter\": \"840\", \"partner\": \"356\", \"product\": 100630, \"year\": \"2020\", \"rate\": 24.0, \"unit\": \"percent\", \"timestamp\": \"2025-10-14T10:30:00\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Missing required parameters",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Tariff not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"timestamp\": \"2025-10-14T10:30:00\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Tariff not found for: reporter=840, partner=356, product=100630, year=2020\", \"path\": \"/api/v1/database/tariffs\"}"
                )
            )
        )
    })
    @GetMapping
    public ResponseEntity<TariffResponse> getTariff(
            @Parameter(description = "Reporter country ISO code (3 characters)", example = "840", required = true)
            @RequestParam @NotNull String reporter,
            
            @Parameter(description = "Partner country ISO code (3 characters)", example = "356", required = true)
            @RequestParam @NotNull String partner,
            
            @Parameter(description = "Product HS code", example = "100630", required = true)
            @RequestParam @NotNull Integer product,
            
            @Parameter(description = "Year (4 digits)", example = "2020", required = true)
            @RequestParam @NotNull @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year) {
        
        logger.info("GET /api/v1/database/tariffs - Getting tariff: reporter={}, partner={}, product={}, year={}",
                reporter, partner, product, year);
        
        TariffResponse response = tariffService.getTariff(reporter, partner, product, year);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Delete tariff rate",
        description = "Deletes a tariff rate by composite key"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Tariff deleted successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Tariff not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"timestamp\": \"2025-10-14T10:30:00\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Tariff not found for: reporter=840, partner=356, product=100630, year=2020\", \"path\": \"/api/v1/database/tariffs\"}"
                )
            )
        )
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteTariff(
            @Parameter(description = "Reporter country ISO code (3 characters)", example = "840", required = true)
            @RequestParam @NotNull String reporter,
            
            @Parameter(description = "Partner country ISO code (3 characters)", example = "356", required = true)
            @RequestParam @NotNull String partner,
            
            @Parameter(description = "Product HS code", example = "100630", required = true)
            @RequestParam @NotNull Integer product,
            
            @Parameter(description = "Year (4 digits)", example = "2020", required = true)
            @RequestParam @NotNull @Pattern(regexp = "\\d{4}", message = "Year must be 4 digits") String year) {
        
        logger.info("DELETE /api/v1/database/tariffs - Deleting tariff: reporter={}, partner={}, product={}, year={}",
                reporter, partner, product, year);
        
        tariffService.deleteTariff(reporter, partner, product, year);
        return ResponseEntity.noContent().build();
    }
}