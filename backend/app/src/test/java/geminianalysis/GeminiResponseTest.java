package geminianalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for GeminiResponse with full branch and statement coverage.
 *
 * Coverage Strategy:
 * - STATEMENT COVERAGE: Every line of code is executed at least once
 * - BRANCH COVERAGE: Every decision point (if/else) is tested in both TRUE and FALSE directions
 *
 * Branch Points in GeminiResponse:
 * 1. Line 56: if (analysisJson != null) in hasJsonAnalysis()
 * 2. Line 64: if (analysisJson != null && analysisJson.has(fieldName)) in getJsonField()
 * 3. Line 66: return field.isTextual() ? field.asText() : field.toString()
 * 4. Line 90: if (success) in toString()
 */
class GeminiResponseTest {

    private ObjectMapper objectMapper;
    private JsonNode sampleJsonNode;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        String jsonString = "{\"summary\":\"Test summary\",\"confidence\":\"high\",\"count\":5}";
        sampleJsonNode = objectMapper.readTree(jsonString);
    }

    // ========================================
    // CONSTRUCTOR TESTS
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 18-23
     * Test: Constructor initializes all fields correctly
     */
    @Test
    void constructor_withAllParameters_initializesFieldsCorrectly() {
        // Arrange & Act
        GeminiResponse response = new GeminiResponse(
            true,
            "Raw response text",
            sampleJsonNode,
            null
        );

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Raw response text", response.getRawResponse());
        assertNotNull(response.getAnalysisJson());
        assertNull(response.getErrorMessage());
    }

    /**
     * STATEMENT COVERAGE: Lines 18-23
     * Test: Constructor with error parameters
     */
    @Test
    void constructor_withErrorParameters_initializesErrorCorrectly() {
        // Arrange & Act
        GeminiResponse response = new GeminiResponse(
            false,
            null,
            null,
            "API error occurred"
        );

        // Assert
        assertFalse(response.isSuccess());
        assertNull(response.getRawResponse());
        assertNull(response.getAnalysisJson());
        assertEquals("API error occurred", response.getErrorMessage());
    }

    // ========================================
    // isSuccess() TESTS
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 28-30
     * Test: isSuccess returns true for successful response
     */
    @Test
    void isSuccess_withSuccessfulResponse_returnsTrue() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text", sampleJsonNode, null);

        // Act & Assert
        assertTrue(response.isSuccess());
    }

    /**
     * STATEMENT COVERAGE: Lines 28-30
     * Test: isSuccess returns false for failed response
     */
    @Test
    void isSuccess_withFailedResponse_returnsFalse() {
        // Arrange
        GeminiResponse response = new GeminiResponse(false, null, null, "Error");

        // Act & Assert
        assertFalse(response.isSuccess());
    }

    // ========================================
    // getRawResponse() TESTS
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 35-37
     * Test: getRawResponse returns the raw text
     */
    @Test
    void getRawResponse_withValidText_returnsText() {
        // Arrange
        String rawText = "This is raw response text";
        GeminiResponse response = new GeminiResponse(true, rawText, null, null);

        // Act & Assert
        assertEquals(rawText, response.getRawResponse());
    }

    /**
     * STATEMENT COVERAGE: Lines 35-37
     * Test: getRawResponse returns null when not provided
     */
    @Test
    void getRawResponse_whenNull_returnsNull() {
        // Arrange
        GeminiResponse response = new GeminiResponse(false, null, null, "Error");

        // Act & Assert
        assertNull(response.getRawResponse());
    }

    // ========================================
    // getAnalysisJson() TESTS
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 42-44
     * Test: getAnalysisJson returns the JSON node
     */
    @Test
    void getAnalysisJson_withValidJson_returnsJsonNode() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text", sampleJsonNode, null);

        // Act & Assert
        assertNotNull(response.getAnalysisJson());
        assertEquals("Test summary", response.getAnalysisJson().get("summary").asText());
    }

    /**
     * STATEMENT COVERAGE: Lines 42-44
     * Test: getAnalysisJson returns null when not provided
     */
    @Test
    void getAnalysisJson_whenNull_returnsNull() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text only", null, null);

        // Act & Assert
        assertNull(response.getAnalysisJson());
    }

    // ========================================
    // getErrorMessage() TESTS
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 49-51
     * Test: getErrorMessage returns error string
     */
    @Test
    void getErrorMessage_withError_returnsErrorString() {
        // Arrange
        String errorMsg = "Connection timeout";
        GeminiResponse response = new GeminiResponse(false, null, null, errorMsg);

        // Act & Assert
        assertEquals(errorMsg, response.getErrorMessage());
    }

    /**
     * STATEMENT COVERAGE: Lines 49-51
     * Test: getErrorMessage returns null for successful response
     */
    @Test
    void getErrorMessage_withSuccess_returnsNull() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text", sampleJsonNode, null);

        // Act & Assert
        assertNull(response.getErrorMessage());
    }

    // ========================================
    // hasJsonAnalysis() TESTS - Branch Point #1
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 56-58
     * BRANCH COVERAGE: Line 56 - TRUE branch (analysisJson != null)
     *
     * Test: hasJsonAnalysis returns true when JSON is present
     */
    @Test
    void hasJsonAnalysis_withJsonPresent_returnsTrue() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text", sampleJsonNode, null);

        // Act & Assert
        assertTrue(response.hasJsonAnalysis());
    }

    /**
     * STATEMENT COVERAGE: Lines 56-58
     * BRANCH COVERAGE: Line 56 - FALSE branch (analysisJson == null)
     *
     * Test: hasJsonAnalysis returns false when JSON is null
     */
    @Test
    void hasJsonAnalysis_withJsonNull_returnsFalse() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text only", null, null);

        // Act & Assert
        assertFalse(response.hasJsonAnalysis());
    }

    // ========================================
    // getJsonField() TESTS - Branch Points #2 and #3
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 63-68
     * BRANCH COVERAGE:
     * - Line 64: TRUE (analysisJson != null && has fieldName)
     * - Line 66: TRUE (field.isTextual())
     *
     * Test: getJsonField returns textual field value
     */
    @Test
    void getJsonField_withTextualField_returnsTextValue() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text", sampleJsonNode, null);

        // Act
        String summary = response.getJsonField("summary");

        // Assert
        assertEquals("Test summary", summary);
    }

    /**
     * STATEMENT COVERAGE: Lines 63-68
     * BRANCH COVERAGE:
     * - Line 64: TRUE (analysisJson != null && has fieldName)
     * - Line 66: FALSE (field is numeric, not textual)
     *
     * Test: getJsonField returns non-textual field as string
     */
    @Test
    void getJsonField_withNumericField_returnsStringRepresentation() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text", sampleJsonNode, null);

        // Act
        String count = response.getJsonField("count");

        // Assert
        assertEquals("5", count);
    }

    /**
     * STATEMENT COVERAGE: Lines 63-68
     * BRANCH COVERAGE: Line 64 - FALSE (analysisJson == null)
     *
     * Test: getJsonField returns null when JSON is null
     */
    @Test
    void getJsonField_withNullJson_returnsNull() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text only", null, null);

        // Act
        String field = response.getJsonField("summary");

        // Assert
        assertNull(field);
    }

    /**
     * STATEMENT COVERAGE: Lines 63-68
     * BRANCH COVERAGE: Line 64 - FALSE (field does not exist)
     *
     * Test: getJsonField returns null for non-existent field
     */
    @Test
    void getJsonField_withNonExistentField_returnsNull() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text", sampleJsonNode, null);

        // Act
        String field = response.getJsonField("nonExistentField");

        // Assert
        assertNull(field);
    }

    /**
     * Edge case: getJsonField with empty string field name
     */
    @Test
    void getJsonField_withEmptyFieldName_returnsNull() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text", sampleJsonNode, null);

        // Act
        String field = response.getJsonField("");

        // Assert
        assertNull(field);
    }

    /**
     * Edge case: getJsonField with complex nested field
     */
    @Test
    void getJsonField_withNestedField_returnsJsonString() throws IOException {
        // Arrange
        String complexJson = "{\"nested\":{\"key\":\"value\"}}";
        JsonNode complexNode = objectMapper.readTree(complexJson);
        GeminiResponse response = new GeminiResponse(true, "text", complexNode, null);

        // Act
        String field = response.getJsonField("nested");

        // Assert
        assertNotNull(field);
        assertTrue(field.contains("key"));
        assertTrue(field.contains("value"));
    }

    // ========================================
    // getSummary() TESTS
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 74-76
     * Test: getSummary returns summary field from JSON
     */
    @Test
    void getSummary_withSummaryInJson_returnsSummary() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text", sampleJsonNode, null);

        // Act
        String summary = response.getSummary();

        // Assert
        assertEquals("Test summary", summary);
    }

    /**
     * STATEMENT COVERAGE: Lines 74-76
     * Test: getSummary returns null when no summary field
     */
    @Test
    void getSummary_withNoSummaryField_returnsNull() throws IOException {
        // Arrange
        String jsonWithoutSummary = "{\"other\":\"field\"}";
        JsonNode node = objectMapper.readTree(jsonWithoutSummary);
        GeminiResponse response = new GeminiResponse(true, "text", node, null);

        // Act
        String summary = response.getSummary();

        // Assert
        assertNull(summary);
    }

    /**
     * STATEMENT COVERAGE: Lines 74-76
     * Test: getSummary returns null when JSON is null
     */
    @Test
    void getSummary_withNullJson_returnsNull() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text only", null, null);

        // Act
        String summary = response.getSummary();

        // Assert
        assertNull(summary);
    }

    // ========================================
    // getConfidence() TESTS
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 81-83
     * Test: getConfidence returns confidence field from JSON
     */
    @Test
    void getConfidence_withConfidenceInJson_returnsConfidence() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text", sampleJsonNode, null);

        // Act
        String confidence = response.getConfidence();

        // Assert
        assertEquals("high", confidence);
    }

    /**
     * STATEMENT COVERAGE: Lines 81-83
     * Test: getConfidence returns null when no confidence field
     */
    @Test
    void getConfidence_withNoConfidenceField_returnsNull() throws IOException {
        // Arrange
        String jsonWithoutConfidence = "{\"summary\":\"test\"}";
        JsonNode node = objectMapper.readTree(jsonWithoutConfidence);
        GeminiResponse response = new GeminiResponse(true, "text", node, null);

        // Act
        String confidence = response.getConfidence();

        // Assert
        assertNull(confidence);
    }

    /**
     * STATEMENT COVERAGE: Lines 81-83
     * Test: getConfidence returns null when JSON is null
     */
    @Test
    void getConfidence_withNullJson_returnsNull() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text only", null, null);

        // Act
        String confidence = response.getConfidence();

        // Assert
        assertNull(confidence);
    }

    // ========================================
    // toString() TESTS - Branch Point #4
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 88-94
     * BRANCH COVERAGE: Line 90 - TRUE branch (success == true)
     *
     * Test: toString returns success message with JSON status
     */
    @Test
    void toString_withSuccessAndJson_returnsSuccessMessage() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text", sampleJsonNode, null);

        // Act
        String result = response.toString();

        // Assert
        assertTrue(result.contains("success=true"));
        assertTrue(result.contains("hasJson=true"));
    }

    /**
     * STATEMENT COVERAGE: Lines 88-94
     * BRANCH COVERAGE: Line 90 - TRUE branch with no JSON
     *
     * Test: toString returns success message without JSON
     */
    @Test
    void toString_withSuccessNoJson_returnsSuccessMessage() {
        // Arrange
        GeminiResponse response = new GeminiResponse(true, "text only", null, null);

        // Act
        String result = response.toString();

        // Assert
        assertTrue(result.contains("success=true"));
        assertTrue(result.contains("hasJson=false"));
    }

    /**
     * STATEMENT COVERAGE: Lines 88-94
     * BRANCH COVERAGE: Line 90 - FALSE branch (success == false)
     *
     * Test: toString returns error message
     */
    @Test
    void toString_withFailure_returnsErrorMessage() {
        // Arrange
        GeminiResponse response = new GeminiResponse(false, null, null, "Test error");

        // Act
        String result = response.toString();

        // Assert
        assertTrue(result.contains("success=false"));
        assertTrue(result.contains("error='Test error'"));
    }

    /**
     * Edge case: toString with very long error message
     */
    @Test
    void toString_withLongErrorMessage_includesFullError() {
        // Arrange
        String longError = "Error: " + "x".repeat(500);
        GeminiResponse response = new GeminiResponse(false, null, null, longError);

        // Act
        String result = response.toString();

        // Assert
        assertTrue(result.contains(longError));
    }

    /**
     * Edge case: toString with null error message
     */
    @Test
    void toString_withNullError_handleGracefully() {
        // Arrange
        GeminiResponse response = new GeminiResponse(false, null, null, null);

        // Act
        String result = response.toString();

        // Assert
        assertTrue(result.contains("success=false"));
        assertTrue(result.contains("error='null'"));
    }
}
