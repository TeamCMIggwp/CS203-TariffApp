package geminianalysis;

import com.fasterxml.jackson.databind.ObjectMapper;
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
            logger.error(
                    "Gemini API key not configured. Set google.api.key property or GOOGLE_API_KEY environment variable");
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
    public ResponseEntity<Map<String, Object>> analyzeData(
            @RequestParam String data,
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
                    request.getPrompt());

            if (geminiResponse.isSuccess()) {
                response.put("success", true);
                response.put("timestamp", System.currentTimeMillis());

                if (geminiResponse.hasJsonAnalysis()) {
                    String prettyAnalysis = objectMapper
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(geminiResponse.getAnalysisJson());

                    response.put("success", true);
                    response.put("analysis", prettyAnalysis); // 👈 always return as string
                } else {
                    response.put("success", true);
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

        public AnalysisRequest() {
        }

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