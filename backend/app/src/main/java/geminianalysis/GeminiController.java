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
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for Gemini AI data analysis operations.
 */
@RestController
@RequestMapping("/gemini")
@Tag(name = "Gemini Analysis", description = "AI-powered data analysis using Google's Gemini API")
public class GeminiController {

    private static final Logger logger = LoggerFactory.getLogger(GeminiController.class);

    @Value("${google.api.key:#{environment.GOOGLE_API_KEY}}")
    private String apiKey;

    private GeminiAnalyzer geminiAnalyzer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initializes the Gemini analyzer after bean construction.
     */
    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("Gemini API key not configured. Set google.api.key property or GOOGLE_API_KEY environment variable");
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
        summary = "Analyze data with Gemini AI",
        description = """
            Analyzes the provided data using Google's Gemini AI and returns structured insights.
            
            **Example URL:** https://teamcmiggwp.duckdns.org/gemini/analyze?data=Trade analysis: Export from 000 to 356. Product: 100630, Value: $123, Year: 2020&prompt=Analyze this agricultural trade data and provide insights on tariff implications, trade relationships, and economic factors
            
            **cURL Example:** 
            
            curl -X GET "https://teamcmiggwp.duckdns.org/gemini/analyze?data=Trade%20analysis%3A%20Export%20from%20000%20to%20356.%20Product%3A%20100630%2C%20Value%3A%20%24123%2C%20Year%3A%202020&prompt=Analyze%20this%20agricultural%20trade%20data%20and%20provide%20insights%20on%20tariff%20implications%2C%20trade%20relationships%2C%20and%20economic%20factors" -H "accept: application/json"""
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully analyzed data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                          "success": true,
                          "timestamp": 1727600000000,
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
            description = "Invalid request - data parameter is required",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "success": false,
                          "error": "Data parameter is required and cannot be empty"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - analysis failed",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "success": false,
                          "error": "Failed to communicate with Gemini API"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Service unavailable - Gemini API error",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "success": false,
                          "error": "Failed to communicate with Gemini API: Connection timeout"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> analyzeData(
            @Parameter(
                description = "Trade or economic data to be analyzed by Gemini AI",
                required = true,
                example = "Trade analysis: Export from 000 to 356. Product: 100630, Value: $123, Year: 2020"
            )
            @RequestParam String data,
            
            @Parameter(
                description = "Custom analysis prompt (optional). If not provided, a default analysis prompt will be used.",
                required = false,
                example = "Analyze this agricultural trade data and provide insights on tariff implications, trade relationships, and economic factors"
            )
            @RequestParam(required = false) String prompt) {

        logger.info("Received analysis request for data: {}", data != null ? "provided" : "missing");

        Map<String, Object> response = new HashMap<>();

        try {
            if (data == null || data.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Data parameter is required and cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Create AnalysisRequest object from parameters
            AnalysisRequest request = new AnalysisRequest(data, prompt);

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

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to communicate with Gemini API: " + e.getMessage());
            logger.error("Exception during analysis", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    /**
     * Health check endpoint for monitoring service status.
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = """
            Check the health status of the Gemini analysis service.
            
            **Example URL:** `https://teamcmiggwp.duckdns.org/gemini/health`
            
            **cURL Example:**

            curl -X GET "https://teamcmiggwp.duckdns.org/gemini/health" -H "accept: application/json"

            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Service is healthy",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": "UP",
                          "service": "gemini-analysis",
                          "timestamp": 1727600000000,
                          "apiKeyConfigured": true
                        }
                        """
                )
            )
        )
    })
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
    public static class AnalysisRequest {
        private String data;
        private String prompt;

        public AnalysisRequest() {}

        public AnalysisRequest(String data, String prompt) {
            this.data = data;
            this.prompt = prompt;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }
    }
}