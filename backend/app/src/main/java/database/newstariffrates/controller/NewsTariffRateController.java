package database.newstariffrates.controller;

import database.newstariffrates.dto.CreateNewsTariffRateRequest;
import database.newstariffrates.dto.NewsTariffRateResponse;
import database.newstariffrates.service.NewsTariffRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tariff-rates")
@Tag(name = "News Tariff Rates", description = "API for managing tariff rates extracted from news articles")
@Validated
public class NewsTariffRateController {

    private static final Logger logger = LoggerFactory.getLogger(NewsTariffRateController.class);

    @Autowired
    private NewsTariffRateService service;

    @Operation(
        summary = "Save tariff rate from news article",
        description = "Creates a tariff rate entry linked to a news article. Ensures one news source = one tariff rate (enforced by unique constraint on news_link)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Tariff rate saved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NewsTariffRateResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error or tariff rate already exists for this news article",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Admin access required",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> createTariffRate(@Valid @RequestBody CreateNewsTariffRateRequest request) {
        logger.info("POST /api/v1/tariff-rates - Creating tariff rate for news: {}", request.getNewsLink());

        try {
            NewsTariffRateResponse response = service.createTariffRate(request);
            return ResponseEntity.status(201).body(response);
        } catch (DataIntegrityViolationException e) {
            logger.warn("Duplicate tariff rate for news: {}", request.getNewsLink());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating tariff rate", e);
            return ResponseEntity.badRequest().body("Failed to create tariff rate: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Get tariff rate by news link",
        description = "Retrieves the tariff rate associated with a specific news article"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tariff rate retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NewsTariffRateResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No tariff rate found for this news article",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping
    public ResponseEntity<?> getTariffRateByNewsLink(
        @Parameter(description = "News article URL", required = true)
        @RequestParam String newsLink
    ) {
        logger.info("GET /api/v1/tariff-rates - Fetching tariff rate for news: {}", newsLink);

        try {
            NewsTariffRateResponse response = service.getTariffRateByNewsLink(newsLink);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @Operation(
        summary = "Check if tariff rate exists for news",
        description = "Checks whether a tariff rate has already been saved for a specific news article"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Check completed",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkTariffRateExists(
        @Parameter(description = "News article URL", required = true)
        @RequestParam String newsLink
    ) {
        logger.info("GET /api/v1/tariff-rates/exists - Checking existence for news: {}", newsLink);
        boolean exists = service.existsByNewsLink(newsLink);
        return ResponseEntity.ok(exists);
    }
}
