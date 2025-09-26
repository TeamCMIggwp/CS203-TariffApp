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
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeData(@RequestBody AnalysisRequest request) {

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

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to communicate with Gemini API: " + e.getMessage());
            logger.error("IOException during analysis", e);
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