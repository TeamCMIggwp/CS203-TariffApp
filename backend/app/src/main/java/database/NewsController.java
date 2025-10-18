package database;

import database.dto.CreateNewsRequest;
import database.dto.UpdateNewsRequest;
import database.dto.NewsResponse;
import database.service.NewsService;

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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/news")
@CrossOrigin(origins = {"http://localhost:3000", "https://teamcmiggwp.duckdns.org"})
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
                    value = "{\"timestamp\": \"2025-10-18T10:30:00\", \"status\": 409, \"error\": \"Conflict\", \"message\": \"News already exists for link: https://www.usitc.gov/rice-report\", \"path\": \"/api/v1/news\"}"
                )
            )
        )
    })
    @PostMapping
    public ResponseEntity<NewsResponse> createNews(@Valid @RequestBody CreateNewsRequest request) {
        
        logger.info("POST /api/v1/news - Creating news: {}", request);
        
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
                    value = "{\"timestamp\": \"2025-10-18T10:30:00\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"News not found for link: https://example.com\", \"path\": \"/api/v1/news\"}"
                )
            )
        )
    })
    @PutMapping
    public ResponseEntity<NewsResponse> updateNews(@Valid @RequestBody UpdateNewsRequest request) {
        
        logger.info("PUT /api/v1/news - Updating news: {}", request.getNewsLink());
        
        NewsResponse response = newsService.updateNews(request.getNewsLink(), request);
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get news article",
        description = "Retrieves a specific news article by link"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "News found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NewsResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "News not found"
        )
    })
    @PostMapping("/get")
    public ResponseEntity<NewsResponse> getNews(@RequestBody Map<String, String> body) {
        
        String newsLink = body.get("newsLink");
        logger.info("POST /api/v1/news/get - Getting news: {}", newsLink);
        
        NewsResponse response = newsService.getNews(newsLink);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get all news articles",
        description = "Retrieves all news articles from the database"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "News list retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NewsResponse.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<List<NewsResponse>> getAllNews() {
        
        logger.info("GET /api/v1/news - Retrieving all news");
        
        List<NewsResponse> responses = newsService.getAllNews();
        return ResponseEntity.ok(responses);
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
            description = "News not found"
        )
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteNews(@RequestBody Map<String, String> body) {
        
        String newsLink = body.get("newsLink");
        logger.info("DELETE /api/v1/news - Deleting: {}", newsLink);
        
        newsService.deleteNews(newsLink);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Check if news exists",
        description = "Checks if a news article exists in the database"
    )
    @PostMapping("/exists")
    public ResponseEntity<Map<String, Boolean>> checkNewsExists(@RequestBody Map<String, String> body) {
        
        String newsLink = body.get("newsLink");
        logger.info("POST /api/v1/news/exists - Checking: {}", newsLink);
        
        boolean exists = newsService.newsExists(newsLink);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
}