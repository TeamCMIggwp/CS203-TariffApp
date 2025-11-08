package database.country.controller;

import database.country.dto.CountryResponse;
import database.country.service.CountryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/v1/countries")
@Tag(name = "Countries", description = "API for retrieving country data")
public class CountryController {
    private static final Logger logger = LoggerFactory.getLogger(CountryController.class);

    @Autowired
    private CountryService countryService;

    @Operation(
        summary = "Get all countries",
        description = "Retrieves all countries from the database ordered by name"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Countries retrieved successfully",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<CountryResponse>> getAllCountries() {
        logger.info("GET /api/v1/countries - Getting all countries");
        List<CountryResponse> response = countryService.getAllCountries();
        return ResponseEntity.ok(response);
    }
}
