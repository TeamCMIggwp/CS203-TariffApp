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
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

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
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Analyzes the provided data using Gemini AI with a custom prompt.
     */
    public GeminiResponse analyzeData(String data, String analysisPrompt) throws IOException {
        String prompt = buildAnalysisPrompt(data, analysisPrompt);
        String requestBody = buildRequestBody(prompt);

        Request request = new Request.Builder()
                .url(GEMINI_API_URL + "?key=" + apiKey)
                .post(RequestBody.create(requestBody, JSON))
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Gemini API request failed with code: {}", response.code());
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
     */
    private String buildRequestBody(String prompt) throws IOException {
        String requestJson = String.format("""
            {
                "contents": [{
                    "parts": [{
                        "text": "%s"
                    }]
                }],
                "generationConfig": {
                    "temperature": 0.3,
                    "topK": 40,
                    "topP": 0.95,
                    "maxOutputTokens": 1024
                }
            }
            """, prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        return requestJson;
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
                JsonNode contentNode = firstCandidate.path("content");
                JsonNode partsNode = contentNode.path("parts");

                if (partsNode.isArray() && partsNode.size() > 0) {
                    String text = partsNode.get(0).path("text").asText();
                    JsonNode analysisJson = parseAnalysisJson(text);

                    return new GeminiResponse(true, text, analysisJson, null);
                }
            }

            return new GeminiResponse(false, null, null, "No valid response from Gemini");

        } catch (Exception e) {
            logger.error("Error parsing Gemini response", e);
            return new GeminiResponse(false, null, null, "Failed to parse response: " + e.getMessage());
        }
    }

    /**
     * Attempts to parse the AI response text as JSON analysis.
     */
    private JsonNode parseAnalysisJson(String text) {
        try {
            String jsonText = text.trim();

            if (jsonText.contains("```json")) {
                int start = jsonText.indexOf("```json") + 7;
                int end = jsonText.indexOf("```", start);
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