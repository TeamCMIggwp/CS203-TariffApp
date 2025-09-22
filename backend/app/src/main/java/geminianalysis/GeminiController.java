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
     */
    @GetMapping("/analyze")
    @Operation(
            summary = "Analyze data using Gemini AI",
            description = "Submits data to Google's Gemini AI for analysis. Returns structured JSON analysis when possible, or raw text analysis otherwise."
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
                            "summary": "Data shows positive trend",
                            "insights": ["Growth rate of 15%", "Peak activity in Q3"],
                            "metrics": {"growth_rate": "15%", "peak_quarter": "Q3"},
                            "recommendations": ["Continue current strategy", "Focus on Q3 optimization"],
                            "confidence": "high"
                          },
                          "summary": "Data shows positive trend",
                          "confidence": "high"
                        }
                        """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - missing or empty data field",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Data field is required and cannot be empty"
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
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Analysis request containing data and optional prompt",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AnalysisRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Basic Analysis",
                                            value = """
                        {
                          "data": "Sales data: Q1: $100k, Q2: $120k, Q3: $135k, Q4: $140k"
                        }
                        """
                                    ),
                                    @ExampleObject(
                                            name = "Custom Prompt",
                                            value = """
                        {
                          "data": "User feedback: 'Love the new features!' 'Great update' 'Could use more customization'",
                          "prompt": "Analyze this customer feedback and identify key themes and sentiment"
                        }
                        """
                                    )
                            }
                    )
            )
            @RequestBody AnalysisRequest request) {

        logger.info("Received analysis request for data type: {}",
                request.getData() != null ? "provided" : "missing");

        Map<String, Object> response = new HashMap<>();

        try {
            if (request.getData() == null || request.getData().trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Data field is required and cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }

            GeminiResponse geminiResponse = geminiAnalyzer.analyzeData(
                    request.getData(),
                    request.getPrompt()
            );

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

                logger.info("Analysis completed successfully");
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
     * Data Transfer Object for analysis requests.
     */
    @Schema(description = "Request object for data analysis")
    public static class AnalysisRequest {

        @Schema(
                description = "The data to be analyzed by Gemini AI",
                example = "Sales data: Q1: $100k, Q2: $120k, Q3: $135k, Q4: $140k",
                required = true
        )
        private String data;

        @Schema(
                description = "Optional custom prompt to guide the AI analysis",
                example = "Analyze this data and provide insights about growth trends"
        )
        private String prompt;

        /**
         * Default constructor for JSON deserialization.
         */
        public AnalysisRequest() {}

        /**
         * Constructs an analysis request with data and prompt.
         */
        public AnalysisRequest(String data, String prompt) {
            this.data = data;
            this.prompt = prompt;
        }

        /**
         * Gets the data to be analyzed.
         */
        public String getData() {
            return data;
        }

        /**
         * Sets the data to be analyzed.
         */
        public void setData(String data) {
            this.data = data;
        }

        /**
         * Gets the analysis prompt.
         */
        public String getPrompt() {
            return prompt;
        }

        /**
         * Sets the analysis prompt.
         */
        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }
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

        @Schema(description = "Brief summary of the analysis", example = "Data shows positive growth trend")
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