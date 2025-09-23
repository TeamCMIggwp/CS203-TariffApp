package geminianalysis;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for Gemini AI data analysis operations.
 */
@RestController
@RequestMapping("/gemini")
@Tag(name = "GeminiAnalysis", description = "APIs for analyzing data using Google's Gemini AI")
public class GeminiController {

    private static final Logger logger = LoggerFactory.getLogger(GeminiController.class);

    @Value("${gemini.api.key:#{environment.GEMINI_API_KEY}}")
    private String apiKey;

    private GeminiAnalyzer geminiAnalyzer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initializes the Gemini analyzer after bean construction.
     */
    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("Gemini API key not configured. Set gemini.api.key property or GEMINI_API_KEY environment variable");
            throw new IllegalStateException("Gemini API key is required");
        }
        this.geminiAnalyzer = new GeminiAnalyzer(apiKey);
        logger.info("GeminiController initialized successfully");
    }

    /**
     * Cleans up resources before bean destruction.
     */
    @PreDestroy
    public void cleanup() {
        if (geminiAnalyzer != null) {
            geminiAnalyzer.close();
            logger.info("GeminiAnalyzer closed");
        }
    }

    /**
     * Analyzes the provided data using Gemini AI.
     * Now accepts data as URL query parameters for easy web browser testing.
     */
    @GetMapping("/analyze")
    @Operation(
            summary = "Analyze data using Gemini AI",
            description = "Submits data to Google's Gemini AI for analysis via URL query parameters. " +
                         "Returns structured JSON analysis when possible, or raw text analysis otherwise. " +
                         "Example: /gemini/analyze?data=china&prompt=analyze this country"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Analysis completed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AnalysisResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Structured Analysis",
                                            value = """
                        {
                          "success": true,
                          "timestamp": 1642778400000,
                          "analysisType": "structured",
                          "analysis": {
                            "summary": "China is the world's most populous country",
                            "insights": ["Population over 1.4 billion", "Major economic power", "Rich cultural history"],
                            "metrics": {"population": "1.4B", "gdp_rank": "2"},
                            "recommendations": ["Focus on sustainable development", "Continue economic reforms"],
                            "confidence": "high"
                          },
                          "summary": "China is the world's most populous country",
                          "confidence": "high"
                        }
                        """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - missing or empty data parameter",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Data parameter is required and cannot be empty"
                }
                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Unexpected error occurred during analysis"
                }
                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service unavailable - Gemini API communication failed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Failed to communicate with Gemini API: Connection timeout"
                }
                """)
                    )
            )
    })
    public ResponseEntity<Map<String, Object>> analyzeData(
            @Parameter(
                    description = "The data to be analyzed by Gemini AI",
                    example = "china",
                    required = true
            )
            @RequestParam String data,
            
            @Parameter(
                    description = "Optional custom prompt to guide the AI analysis",
                    example = "Analyze this country and provide demographic and economic insights"
            )
            @RequestParam(required = false) String prompt) {

        logger.info("Received GET analysis request for data: '{}'", 
                data != null && data.length() > 50 ? data.substring(0, 50) + "..." : data);

        Map<String, Object> response = new HashMap<>();

        try {
            // URL decode the data parameter in case it contains special characters
            String decodedData = URLDecoder.decode(data, StandardCharsets.UTF_8);
            
            if (decodedData == null || decodedData.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Data parameter is required and cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }

            // URL decode the prompt if provided
            String decodedPrompt = null;
            if (prompt != null && !prompt.trim().isEmpty()) {
                decodedPrompt = URLDecoder.decode(prompt, StandardCharsets.UTF_8);
            }

            GeminiResponse geminiResponse = geminiAnalyzer.analyzeData(decodedData, decodedPrompt);

            if (geminiResponse.isSuccess()) {
                response.put("success", true);
                response.put("timestamp", System.currentTimeMillis());

                if (geminiResponse.hasJsonAnalysis()) {
                    response.put("analysisType", "structured");
                    response.put("analysis", geminiResponse.getAnalysisJson());
                    response.put("summary", geminiResponse.getSummary());
                    response.put("confidence", geminiResponse.getConfidence());
                } else {
                    response.put("analysisType", "text");
                    response.put("analysis", geminiResponse.getRawResponse());
                }

                logger.info("Analysis completed successfully for data: '{}'", 
                        decodedData.length() > 20 ? decodedData.substring(0, 20) + "..." : decodedData);
                return ResponseEntity.ok(response);

            } else {
                response.put("success", false);
                response.put("error", geminiResponse.getErrorMessage());
                logger.warn("Analysis failed: {}", geminiResponse.getErrorMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (IOException e) {
            response.put("success", false);
            response.put("error", "Failed to communicate with Gemini API: " + e.getMessage());
            logger.error("IOException during analysis", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Unexpected error occurred during analysis");
            logger.error("Unexpected error during analysis", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Health check endpoint for monitoring service status.
     */
    @GetMapping("/health")
    @Operation(
            summary = "Health check",
            description = "Returns the current health status of the Gemini analysis service"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Service is healthy",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = HealthResponse.class),
                    examples = @ExampleObject(value = """
            {
              "status": "UP",
              "service": "gemini-analysis",
              "timestamp": 1642778400000,
              "apiKeyConfigured": true
            }
            """)
            )
    )
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "gemini-analysis");
        health.put("timestamp", System.currentTimeMillis());
        health.put("apiKeyConfigured", apiKey != null && !apiKey.trim().isEmpty());
        return ResponseEntity.ok(health);
    }

    /**
     * Response schema for analysis operations.
     */
    @Schema(description = "Response object for analysis operations")
    public static class AnalysisResponse {

        @Schema(description = "Whether the analysis was successful", example = "true")
        public boolean success;

        @Schema(description = "Timestamp of the analysis", example = "1642778400000")
        public long timestamp;

        @Schema(description = "Type of analysis returned", allowableValues = {"structured", "text"}, example = "structured")
        public String analysisType;

        @Schema(description = "The analysis result (JSON object for structured, string for text)")
        public Object analysis;

        @Schema(description = "Brief summary of the analysis", example = "China is the world's most populous country")
        public String summary;

        @Schema(description = "Confidence level of the analysis", allowableValues = {"high", "medium", "low"}, example = "high")
        public String confidence;

        @Schema(description = "Error message if analysis failed", example = "Failed to communicate with Gemini API")
        public String error;
    }

    /**
     * Response schema for health check operations.
     */
    @Schema(description = "Response object for health check")
    public static class HealthResponse {

        @Schema(description = "Service status", example = "UP")
        public String status;

        @Schema(description = "Service name", example = "gemini-analysis")
        public String service;

        @Schema(description = "Timestamp of the health check", example = "1642778400000")
        public long timestamp;

        @Schema(description = "Whether API key is properly configured", example = "true")
        public boolean apiKeyConfigured;
    }
}