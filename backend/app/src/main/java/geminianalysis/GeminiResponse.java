package geminianalysis;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Immutable response wrapper for Gemini AI analysis results.
 */
public class GeminiResponse {

    private final boolean success;
    private final String rawResponse;
    private final JsonNode analysisJson;
    private final String errorMessage;

    /**
     * Constructs a new GeminiResponse with the specified parameters.
     */
    public GeminiResponse(boolean success, String rawResponse, JsonNode analysisJson, String errorMessage) {
        this.success = success;
        this.rawResponse = rawResponse;
        this.analysisJson = analysisJson;
        this.errorMessage = errorMessage;
    }

    /**
     * Indicates whether the analysis operation was successful.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the raw text response from Gemini AI.
     */
    public String getRawResponse() {
        return rawResponse;
    }

    /**
     * Returns the parsed JSON analysis structure.
     */
    public JsonNode getAnalysisJson() {
        return analysisJson;
    }

    /**
     * Returns the error message if the operation failed.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Convenience method to check if structured JSON analysis is available.
     */
    public boolean hasJsonAnalysis() {
        return analysisJson != null;
    }

    /**
     * Convenience method to extract a specific field from the JSON analysis.
     */
    public String getJsonField(String fieldName) {
        if (analysisJson != null && analysisJson.has(fieldName)) {
            JsonNode field = analysisJson.get(fieldName);
            return field.isTextual() ? field.asText() : field.toString();
        }
        return null;
    }

    /**
     * Convenience method to get the analysis summary.
     */
    public String getSummary() {
        return getJsonField("summary");
    }

    /**
     * Convenience method to get the confidence level.
     */
    public String getConfidence() {
        return getJsonField("confidence");
    }

    /**
     * Returns a string representation of this response.
     */
    @Override
    public String toString() {
        if (success) {
            return String.format("GeminiResponse{success=true, hasJson=%s}", hasJsonAnalysis());
        } else {
            return String.format("GeminiResponse{success=false, error='%s'}", errorMessage);
        }
    }
}