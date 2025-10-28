package database.news.controller;

import database.news.dto.CreateNewsRequest;
import database.news.dto.UpdateNewsRequest;
import database.news.dto.NewsResponse;
import database.news.service.NewsService;

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
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@RestController
@RequestMapping("/api/v1/news")
@Tag(name = "News Management", description = "RESTful API for managing news articles")
@Validated
public class NewsController {
    private static final Logger logger = LoggerFactory.getLogger(NewsController.class);

    @Autowired
    private NewsService newsService;

    @Operation(
        summary = "Create new news article",
        description = "Creates a new news article. Returns 409 Conflict if news link already exists."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "News created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NewsResponse.class),
                examples = @ExampleObject(
                    value = "{\"newsLink\": \"https://www.usitc.gov/rice-report\", \"remarks\": \"Important report on rice tariffs\", \"timestamp\": \"2025-10-18T10:30:00\"}"
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
            description = "News already exists",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"timestamp\": \"2025-10-18T10:30:00\", \"status\": 409, \"error\": \"Conflict\", \"message\": \"News already exists for link: https://www.usitc.gov/rice-report\", \"path\": \"/api/v1/database/news\"}"
                )
            )
        )
    })
    @PostMapping
    public ResponseEntity<NewsResponse> createNews(@Valid @RequestBody CreateNewsRequest request) {
        
        logger.info("POST /api/v1/database/news - Creating news: {}", request);
        
        NewsResponse response = newsService.createNews(request);
        
        return ResponseEntity.status(201).body(response);
    }

    @Operation(
        summary = "Update existing news remarks",
        description = "Updates remarks for an existing news article. Returns 404 if news doesn't exist."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "News updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NewsResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "News not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"timestamp\": \"2025-10-18T10:30:00\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"News not found for link: https://example.com\", \"path\": \"/api/v1/database/news\"}"
                )
            )
        )
    })
    @PutMapping
    public ResponseEntity<NewsResponse> updateNews(@Valid @RequestBody UpdateNewsRequest request) {
        
        logger.info("PUT /api/v1/database/news - Updating news: {}", request.getNewsLink());
        
        NewsResponse response = newsService.updateNews(request.getNewsLink(), request);
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get specific news article",
        description = "Get specific news article by providing newsLink as query parameter. Parameter is required."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "News retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"newsLink\": \"https://www.usitc.gov/rice-report\", \"remarks\": \"Important report on rice tariffs\", \"timestamp\": \"2025-10-18T10:30:00\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Missing required parameter",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "News not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"timestamp\": \"2025-10-18T10:30:00\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"News not found for link: https://example.com\", \"path\": \"/api/v1/database/news\"}"
                )
            )
        )
    })
    @GetMapping
    public ResponseEntity<NewsResponse> getNews(
            @Parameter(description = "News article URL", example = "https://www.usitc.gov/rice-report", required = true)
            @RequestParam @NotBlank String newsLink) {
        
        logger.info("GET /api/v1/database/news - Getting news: {}", newsLink);
        NewsResponse response = newsService.getNews(newsLink);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Delete news article",
        description = "Deletes a news article from the database"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "News deleted successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "News not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"timestamp\": \"2025-10-18T10:30:00\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"News not found for link: https://example.com\", \"path\": \"/api/v1/database/news\"}"
                )
            )
        )
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteNews(
            @Parameter(description = "News article URL", example = "https://www.usitc.gov/rice-report", required = true)
            @RequestParam @NotBlank String newsLink) {

        logger.info("DELETE /api/v1/database/news - Deleting: {}", newsLink);

        newsService.deleteNews(newsLink);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Hide news source",
        description = "Marks a news source as hidden. If the source doesn't exist in the database, it will be created as hidden. Hidden sources won't appear in search results."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "News source hidden successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NewsResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Missing required parameter",
            content = @Content(mediaType = "application/json")
        )
    })
    @PatchMapping("/hide")
    public ResponseEntity<NewsResponse> hideSource(
            @Parameter(description = "News article URL to hide", example = "https://www.usitc.gov/rice-report", required = true)
            @RequestParam @NotBlank String newsLink) {

        logger.info("PATCH /api/v1/news/hide - Hiding source: {}", newsLink);

        NewsResponse response = newsService.hideSource(newsLink);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Unhide news source",
        description = "Marks a news source as visible again. Returns 404 if the source doesn't exist."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "News source unhidden successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NewsResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "News not found",
            content = @Content(mediaType = "application/json")
        )
    })
    @PatchMapping("/unhide")
    public ResponseEntity<NewsResponse> unhideSource(
            @Parameter(description = "News article URL to unhide", example = "https://www.usitc.gov/rice-report", required = true)
            @RequestParam @NotBlank String newsLink) {

        logger.info("PATCH /api/v1/news/unhide - Unhiding source: {}", newsLink);

        NewsResponse response = newsService.unhideSource(newsLink);
        return ResponseEntity.ok(response);
    }
}