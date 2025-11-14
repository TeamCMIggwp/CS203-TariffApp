package geminianalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Service class for analyzing data using Google's Gemini AI API.
 */
public class GeminiAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(GeminiAnalyzer.class);
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int CONNECT_TIMEOUT_SECONDS = 30;
    private static final int WRITE_TIMEOUT_SECONDS = 30;
    private static final int READ_TIMEOUT_SECONDS = 60;
    private static final String API_KEY_HEADER = "x-goog-api-key";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";

    // Gemini API generation config defaults
    private static final double DEFAULT_TEMPERATURE = 0.3;
    private static final int DEFAULT_TOP_K = 40;
    private static final double DEFAULT_TOP_P = 0.95;
    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 8192;

    // Response parsing constants
    private static final String JSON_CODE_BLOCK_START = "```json";
    private static final String CODE_BLOCK_START = "```";
    private static final int JSON_PREFIX_LENGTH = 7; // Length of "```json"
    private static final int CODE_PREFIX_LENGTH = 3; // Length of "```"

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    /**
     * Constructs a new GeminiAnalyzer with the specified API key.
     */
    public GeminiAnalyzer(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }

        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Analyzes the provided data using Gemini AI with a custom prompt.
     */
    public GeminiResponse analyzeData(String data, String analysisPrompt) throws IOException {
        String prompt = buildAnalysisPrompt(data, analysisPrompt);
        String requestBody = buildRequestBody(prompt);

        Request request = new Request.Builder()
                .url(GEMINI_API_URL)
                .post(RequestBody.create(requestBody, JSON))
                .addHeader(CONTENT_TYPE_HEADER, "application/json")
                .addHeader(API_KEY_HEADER, apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Gemini API request failed with code: {} - Response: {}", 
                    response.code(), response.body() != null ? response.body().string() : "No body");
                throw new IOException("Unexpected response code: " + response.code());
            }

            String responseBody = response.body().string();
            return parseGeminiResponse(responseBody);
        }
    }

    /**
     * Analyzes data using Gemini AI with default prompting.
     */
    public GeminiResponse analyzeData(String data) throws IOException {
        return analyzeData(data, "Analyze this data and provide insights in JSON format");
    }

    /**
     * Builds the complete analysis prompt for Gemini AI.
     */
    private String buildAnalysisPrompt(String data, String analysisPrompt) {
        String basePrompt = analysisPrompt != null ? analysisPrompt :
                "Analyze the following data and provide insights";

        return String.format("""
            %s. Please respond with a JSON object containing your analysis.
            
            Data to analyze:
            %s
            
            Respond only with valid JSON in the following structure:
            {
                "summary": "Brief summary of the data",
                "insights": ["insight 1", "insight 2", "..."],
                "metrics": {
                    "key_metric_1": "value",
                    "key_metric_2": "value"
                },
                "recommendations": ["recommendation 1", "recommendation 2"],
                "confidence": "high/medium/low"
            }
            """, basePrompt, data);
    }

    /**
     * Builds the JSON request body for the Gemini API.
     * Updated to use the new contents structure expected by Gemini API
     */
    private String buildRequestBody(String prompt) {
        // Escape the prompt text properly for JSON
        String escapedPrompt = prompt.replace("\\", "\\\\")
                                   .replace("\"", "\\\"")
                                   .replace("\n", "\\n")
                                   .replace("\r", "\\r")
                                   .replace("\t", "\\t");

        return String.format("""
            {
                "contents": [
                    {
                        "parts": [
                            {
                                "text": "%s"
                            }
                        ]
                    }
                ],
                "generationConfig": {
                    "temperature": %.1f,
                    "topK": %d,
                    "topP": %.2f,
                    "maxOutputTokens": %d
                }
            }
            """, escapedPrompt, DEFAULT_TEMPERATURE, DEFAULT_TOP_K, DEFAULT_TOP_P, DEFAULT_MAX_OUTPUT_TOKENS);
    }

    /**
     * Parses the response from Gemini API into a GeminiResponse object.
     */
    private GeminiResponse parseGeminiResponse(String responseBody) throws IOException {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode candidatesNode = rootNode.path("candidates");

            if (candidatesNode.isArray() && candidatesNode.size() > 0) {
                JsonNode firstCandidate = candidatesNode.get(0);

                // Check if response was cut off due to MAX_TOKENS
                String finishReason = firstCandidate.path("finishReason").asText("");
                if ("MAX_TOKENS".equals(finishReason)) {
                    logger.warn("Gemini response hit MAX_TOKENS limit. Consider increasing maxOutputTokens.");
                    return new GeminiResponse(false, null, null,
                        "Response was cut off due to token limit. Please try with a shorter prompt or increase maxOutputTokens.");
                }

                JsonNode contentNode = firstCandidate.path("content");
                JsonNode partsNode = contentNode.path("parts");

                if (partsNode.isArray() && partsNode.size() > 0) {
                    String text = partsNode.get(0).path("text").asText();
                    JsonNode analysisJson = parseAnalysisJson(text);

                    return new GeminiResponse(true, text, analysisJson, null);
                }
            }

            // Log the full response for debugging if parsing fails
            logger.warn("Failed to parse response structure. Full response: {}", responseBody);
            return new GeminiResponse(false, null, null, "No valid response from Gemini");

        } catch (Exception e) {
            logger.error("Error parsing Gemini response: {}", responseBody, e);
            return new GeminiResponse(false, null, null, "Failed to parse response: " + e.getMessage());
        }
    }

    /**
     * Attempts to parse the AI response text as JSON analysis.
     */
    private JsonNode parseAnalysisJson(String text) {
        try {
            String jsonText = text.trim();

            // Handle markdown code blocks
            if (jsonText.contains(JSON_CODE_BLOCK_START)) {
                int start = jsonText.indexOf(JSON_CODE_BLOCK_START) + JSON_PREFIX_LENGTH;
                int end = jsonText.indexOf(CODE_BLOCK_START, start);
                if (end > start) {
                    jsonText = jsonText.substring(start, end).trim();
                }
            } else if (jsonText.contains(CODE_BLOCK_START)) {
                // Handle generic code blocks
                int start = jsonText.indexOf(CODE_BLOCK_START) + CODE_PREFIX_LENGTH;
                int end = jsonText.indexOf(CODE_BLOCK_START, start);
                if (end > start) {
                    jsonText = jsonText.substring(start, end).trim();
                }
            }

            if (jsonText.startsWith("{") && jsonText.endsWith("}")) {
                return objectMapper.readTree(jsonText);
            }

            return null;
        } catch (Exception e) {
            logger.warn("Could not parse analysis as JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Closes the analyzer and cleans up HTTP client resources.
     */
    public void close() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }
}