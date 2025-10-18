package database;

import database.dto.CreateTariffRequest;
import database.dto.UpdateTariffRequest;
import database.dto.GetTariffRequest;
import database.dto.TariffResponse;
import database.service.TariffService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/database/tariffs")
@CrossOrigin(origins = { "http://localhost:3000", "https://teamcmiggwp.duckdns.org" })
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
        summary = "Get tariff rate(s)",
        description = "Get all tariffs if no body provided, or get specific tariff if body contains reporter, partner, product, and year"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tariff(s) retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TariffResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Tariff not found (when requesting specific tariff)"
        )
    })
    @GetMapping
    public ResponseEntity<?> getTariff(@RequestBody(required = false) GetTariffRequest request) {
        
        // If no body provided, return all tariffs
        if (request == null || request.getReporter() == null) {
            logger.info("GET /api/v1/database/tariffs - Retrieving all tariffs");
            List<TariffResponse> responses = tariffService.getAllTariffs();
            return ResponseEntity.ok(responses);
        }
        
        // If body provided, return specific tariff
        logger.info("GET /api/v1/database/tariffs - Getting tariff: reporter={}, partner={}, product={}, year={}",
                request.getReporter(), request.getPartner(), request.getProduct(), request.getYear());
        
        TariffResponse response = tariffService.getTariff(
                request.getReporter(),
                request.getPartner(),
                request.getProduct(),
                request.getYear());
        
        return ResponseEntity.ok(response);
    }
}