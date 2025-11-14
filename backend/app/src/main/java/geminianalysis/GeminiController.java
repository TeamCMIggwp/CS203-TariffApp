package geminianalysis;

import geminianalysis.dto.AnalysisRequest;
import geminianalysis.dto.AnalysisResponse;
import geminianalysis.service.GeminiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * REST controller for Gemini AI data analysis operations.
 */
@RestController
@RequestMapping("/api/v1/gemini")
@Tag(name = "Gemini Analysis", description = "AI-powered data analysis using Google's Gemini API")
@Validated
public class GeminiController {

    private static final Logger logger = LoggerFactory.getLogger(GeminiController.class);

    @Autowired
    private GeminiService geminiService;

    /**
     * Create a new analysis (POST /api/v1/gemini/analyses)
     */
    @PostMapping("/analyses")
    @Operation(
        summary = "Analyze data with Gemini AI",
        description = """
            Analyzes the provided data using Google's Gemini AI and returns structured insights.
            This is a stateless operation that processes the data and returns results immediately.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Analysis completed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AnalysisResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "success": true,
                          "timestamp": "2025-10-14T10:30:00",
                          "analysisType": "structured",
                          "analysis": {
                            "summary": "Agricultural trade analysis shows significant export activity",
                            "insights": ["Export value of $123 for product 100630", "Trade flow from country 000 to 356"],
                            "metrics": {
                              "exportValue": "$123",
                              "year": "2020",
                              "product": "100630"
                            },
                            "recommendations": ["Consider tariff implications", "Analyze trade relationships"],
                            "confidence": "high"
                          },
                          "summary": "Agricultural trade analysis shows significant export activity",
                          "confidence": "high"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation failed",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "success": false,
                          "timestamp": "2025-10-14T10:30:00",
                          "error": "Data to analyze is required"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "success": false,
                          "timestamp": "2025-10-14T10:30:00",
                          "error": "Failed to communicate with Gemini API"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<AnalysisResponse> createAnalysis(
            @Valid @RequestBody AnalysisRequest request) {

        logger.info("POST /api/v1/gemini/analyses - Received analysis request");

        AnalysisResponse response = geminiService.analyzeData(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}