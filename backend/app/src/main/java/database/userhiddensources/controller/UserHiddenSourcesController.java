package database.userhiddensources.controller;

import database.userhiddensources.dto.HiddenSourceResponse;
import database.userhiddensources.service.UserHiddenSourcesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user/hidden-sources")
@Tag(name = "User Hidden Sources", description = "API for regular users to manage their personal hidden news sources")
@Validated
public class UserHiddenSourcesController {
    private static final Logger logger = LoggerFactory.getLogger(UserHiddenSourcesController.class);

    @Autowired
    private UserHiddenSourcesService service;

    /**
     * Get user identifier from JWT token
     * Note: authentication.getName() returns the userId (not email) from the JWT
     * We use userId as the identifier since it's unique and consistent
     */
    private String getUserIdentifier() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName(); // This returns userId from JWT
        }
        throw new RuntimeException("User not authenticated");
    }

    @Operation(
        summary = "Hide a news source (User)",
        description = "Hides a news source for the current user. Does not affect other users or admins."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Source hidden successfully",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/hide")
    public ResponseEntity<HiddenSourceResponse> hideSource(
            @Parameter(description = "News article URL to hide", example = "https://www.wto.org/article", required = true)
            @RequestParam @NotBlank String newsLink) {

        String userId = getUserIdentifier();
        logger.info("POST /api/v1/user/hidden-sources/hide - User: {}, newsLink: {}", userId, newsLink);

        HiddenSourceResponse response = service.hideSourceForUser(userId, newsLink);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Unhide a news source (User)",
        description = "Unhides a previously hidden news source for the current user."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Source unhidden successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content(mediaType = "application/json")
        )
    })
    @DeleteMapping("/unhide")
    public ResponseEntity<Void> unhideSource(
            @Parameter(description = "News article URL to unhide", example = "https://www.wto.org/article", required = true)
            @RequestParam @NotBlank String newsLink) {

        String userId = getUserIdentifier();
        logger.info("DELETE /api/v1/user/hidden-sources/unhide - User: {}, newsLink: {}", userId, newsLink);

        service.unhideSourceForUser(userId, newsLink);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Unhide all sources (User)",
        description = "Unhides all hidden sources for the current user."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "All sources unhidden successfully",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content(mediaType = "application/json")
        )
    })
    @DeleteMapping("/unhide-all")
    public ResponseEntity<String> unhideAllSources() {

        String userId = getUserIdentifier();
        logger.info("DELETE /api/v1/user/hidden-sources/unhide-all - User: {}", userId);

        int count = service.unhideAllSourcesForUser(userId);
        return ResponseEntity.ok(String.format("Unhidden %d sources", count));
    }

    @Operation(
        summary = "Get all hidden sources (User)",
        description = "Retrieves all hidden sources for the current user."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Hidden sources retrieved successfully",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping
    public ResponseEntity<List<HiddenSourceResponse>> getHiddenSources() {

        String userId = getUserIdentifier();
        logger.info("GET /api/v1/user/hidden-sources - User: {}", userId);

        List<HiddenSourceResponse> response = service.getHiddenSourcesForUser(userId);
        return ResponseEntity.ok(response);
    }
}
