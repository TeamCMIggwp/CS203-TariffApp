package geminianalysis;

import geminianalysis.dto.AnalysisRequest;
import geminianalysis.dto.AnalysisResponse;
import geminianalysis.service.GeminiService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for GeminiService with full branch and statement coverage.
 *
 * Coverage Strategy:
 * - STATEMENT COVERAGE: Every line of code is executed at least once
 * - BRANCH COVERAGE: Every decision point (if/else) is tested in both TRUE and FALSE directions
 *
 * Branch Points in GeminiService.analyzeData():
 * 1. Line 35: if (geminiResponse.isSuccess())
 * 2. Line 36: analysisType = geminiResponse.hasJsonAnalysis() ? "structured" : "text"
 * 3. Line 37-39: analysis = geminiResponse.hasJsonAnalysis() ? getAnalysisJson() : getRawResponse()
 * 4. Line 60: catch (IOException e)
 * 5. Line 63: catch (Exception e)
 */
@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private GeminiAnalyzer geminiAnalyzer;

    @InjectMocks
    private GeminiService geminiService;

    private AnalysisRequest validRequest;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validRequest = new AnalysisRequest();
        validRequest.setData("Test agricultural trade data");
        validRequest.setPrompt("Analyze this trade data");

        objectMapper = new ObjectMapper();
    }

    // ========================================
    // TEST: analyzeData() - SUCCESS WITH JSON
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 27, 29-33, 35-51
     * BRANCH COVERAGE:
     * - Line 35: TRUE (geminiResponse.isSuccess() == true)
     * - Line 36: TRUE (hasJsonAnalysis() == true) -> "structured"
     * - Line 37: TRUE (hasJsonAnalysis() == true) -> getAnalysisJson()
     */
    @Test
    void analyzeData_withSuccessfulJsonResponse_returnsStructuredAnalysis() throws IOException {
        // Arrange
        String jsonString = "{\"summary\":\"Trade analysis\",\"confidence\":\"high\"}";
        JsonNode jsonNode = objectMapper.readTree(jsonString);

        GeminiResponse geminiResponse = new GeminiResponse(
            true,
            "{\"summary\":\"Trade analysis\",\"confidence\":\"high\"}",
            jsonNode,
            null
        );

        when(geminiAnalyzer.analyzeData(anyString(), anyString())).thenReturn(geminiResponse);

        // Act
        AnalysisResponse response = geminiService.analyzeData(validRequest);

        // Assert - Statement Coverage
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("structured", response.getAnalysisType());
        assertNotNull(response.getAnalysis());
        assertEquals("Trade analysis", response.getSummary());
        assertEquals("high", response.getConfidence());
        assertNotNull(response.getId());
        assertNull(response.getError());

        // Verify analyzer interaction
        verify(geminiAnalyzer, times(1)).analyzeData("Test agricultural trade data", "Analyze this trade data");
    }

    // ========================================
    // TEST: analyzeData() - SUCCESS WITH TEXT ONLY
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 27, 29-33, 35-51
     * BRANCH COVERAGE:
     * - Line 35: TRUE (geminiResponse.isSuccess() == true)
     * - Line 36: FALSE (hasJsonAnalysis() == false) -> "text"
     * - Line 37: FALSE (hasJsonAnalysis() == false) -> getRawResponse()
     */
    @Test
    void analyzeData_withSuccessfulTextResponse_returnsTextAnalysis() throws IOException {
        // Arrange
        GeminiResponse geminiResponse = new GeminiResponse(
            true,
            "Plain text analysis result without JSON structure",
            null,  // No JSON analysis
            null
        );

        when(geminiAnalyzer.analyzeData(anyString(), anyString())).thenReturn(geminiResponse);

        // Act
        AnalysisResponse response = geminiService.analyzeData(validRequest);

        // Assert - Branch Coverage for text analysis
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("text", response.getAnalysisType());
        assertEquals("Plain text analysis result without JSON structure", response.getAnalysis());
        assertNull(response.getSummary());  // No summary in text-only response
        assertNull(response.getConfidence()); // No confidence in text-only response
        assertNotNull(response.getId());

        verify(geminiAnalyzer, times(1)).analyzeData(anyString(), anyString());
    }

    // ========================================
    // TEST: analyzeData() - GEMINI FAILURE
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 27, 29-33, 35, 55-57
     * BRANCH COVERAGE:
     * - Line 35: FALSE (geminiResponse.isSuccess() == false)
     */
    @Test
    void analyzeData_withGeminiFailure_returnsErrorResponse() throws IOException {
        // Arrange
        GeminiResponse geminiResponse = new GeminiResponse(
            false,
            null,
            null,
            "Gemini API returned error: Invalid API key"
        );

        when(geminiAnalyzer.analyzeData(anyString(), anyString())).thenReturn(geminiResponse);

        // Act
        AnalysisResponse response = geminiService.analyzeData(validRequest);

        // Assert - Branch Coverage for failure path
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Gemini API returned error: Invalid API key", response.getError());
        assertNull(response.getAnalysis());
        assertNull(response.getAnalysisType());
        assertNull(response.getId()); // No ID set for error responses

        verify(geminiAnalyzer, times(1)).analyzeData(anyString(), anyString());
    }

    // ========================================
    // TEST: analyzeData() - IOException
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 27, 29-33, 60-62
     * BRANCH COVERAGE:
     * - Catch block at line 60: TRUE (IOException thrown)
     */
    @Test
    void analyzeData_withIOException_returnsErrorResponse() throws IOException {
        // Arrange
        when(geminiAnalyzer.analyzeData(anyString(), anyString()))
            .thenThrow(new IOException("Network error: Connection timeout"));

        // Act
        AnalysisResponse response = geminiService.analyzeData(validRequest);

        // Assert - Statement Coverage for IOException handler
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.getError().contains("Failed to communicate with Gemini API"));
        assertTrue(response.getError().contains("Network error: Connection timeout"));

        verify(geminiAnalyzer, times(1)).analyzeData(anyString(), anyString());
    }

    // ========================================
    // TEST: analyzeData() - Generic Exception
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 27, 29-33, 63-65
     * BRANCH COVERAGE:
     * - Catch block at line 63: TRUE (Exception thrown)
     */
    @Test
    void analyzeData_withGenericException_returnsErrorResponse() throws IOException {
        // Arrange
        when(geminiAnalyzer.analyzeData(anyString(), anyString()))
            .thenThrow(new RuntimeException("Unexpected error occurred"));

        // Act
        AnalysisResponse response = geminiService.analyzeData(validRequest);

        // Assert - Statement Coverage for generic exception handler
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.getError().contains("Unexpected error"));
        assertTrue(response.getError().contains("Unexpected error occurred"));

        verify(geminiAnalyzer, times(1)).analyzeData(anyString(), anyString());
    }

    // ========================================
    // EDGE CASE TESTS
    // ========================================

    /**
     * Edge case: Request with null prompt (analyzer should handle gracefully)
     */
    @Test
    void analyzeData_withNullPrompt_usesDataParameter() throws IOException {
        // Arrange
        AnalysisRequest requestWithNullPrompt = new AnalysisRequest();
        requestWithNullPrompt.setData("Trade data");
        requestWithNullPrompt.setPrompt(null);

        String jsonString = "{\"summary\":\"Analysis\"}";
        JsonNode jsonNode = objectMapper.readTree(jsonString);

        GeminiResponse geminiResponse = new GeminiResponse(true, "{}", jsonNode, null);
        when(geminiAnalyzer.analyzeData(anyString(), isNull())).thenReturn(geminiResponse);

        // Act
        AnalysisResponse response = geminiService.analyzeData(requestWithNullPrompt);

        // Assert
        assertTrue(response.isSuccess());
        verify(geminiAnalyzer, times(1)).analyzeData("Trade data", null);
    }

    /**
     * Edge case: JSON response with null summary and confidence
     */
    @Test
    void analyzeData_withNullSummaryAndConfidence_handlesGracefully() throws IOException {
        // Arrange
        String jsonString = "{\"insights\":[\"insight1\",\"insight2\"]}";
        JsonNode jsonNode = objectMapper.readTree(jsonString);

        GeminiResponse geminiResponse = new GeminiResponse(true, "{}", jsonNode, null);
        when(geminiAnalyzer.analyzeData(anyString(), anyString())).thenReturn(geminiResponse);

        // Act
        AnalysisResponse response = geminiService.analyzeData(validRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertNull(response.getSummary());
        assertNull(response.getConfidence());
        assertNotNull(response.getAnalysis());
    }

    /**
     * Edge case: Empty data string (should still process)
     */
    @Test
    void analyzeData_withEmptyData_processesRequest() throws IOException {
        // Arrange
        AnalysisRequest emptyDataRequest = new AnalysisRequest();
        emptyDataRequest.setData("");
        emptyDataRequest.setPrompt("Analyze");

        GeminiResponse geminiResponse = new GeminiResponse(true, "No data provided", null, null);
        when(geminiAnalyzer.analyzeData(anyString(), anyString())).thenReturn(geminiResponse);

        // Act
        AnalysisResponse response = geminiService.analyzeData(emptyDataRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("text", response.getAnalysisType());
        verify(geminiAnalyzer, times(1)).analyzeData("", "Analyze");
    }

    /**
     * Edge case: Very long error message
     */
    @Test
    void analyzeData_withLongErrorMessage_returnsFullError() throws IOException {
        // Arrange
        String longError = "Error message ".repeat(100); // Very long error
        GeminiResponse geminiResponse = new GeminiResponse(false, null, null, longError);
        when(geminiAnalyzer.analyzeData(anyString(), anyString())).thenReturn(geminiResponse);

        // Act
        AnalysisResponse response = geminiService.analyzeData(validRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(longError, response.getError());
    }

    /**
     * Edge case: NullPointerException during processing
     */
    @Test
    void analyzeData_withNullPointerException_handlesGracefully() throws IOException {
        // Arrange
        when(geminiAnalyzer.analyzeData(anyString(), anyString()))
            .thenThrow(new NullPointerException("Null value encountered"));

        // Act
        AnalysisResponse response = geminiService.analyzeData(validRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertTrue(response.getError().contains("Unexpected error"));
        assertTrue(response.getError().contains("Null value encountered"));
    }

    /**
     * Edge case: Multiple sequential calls should generate unique IDs
     */
    @Test
    void analyzeData_multipleSuccessfulCalls_generatesUniqueIds() throws IOException {
        // Arrange
        String jsonString = "{\"summary\":\"Analysis\"}";
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        GeminiResponse geminiResponse = new GeminiResponse(true, "{}", jsonNode, null);
        when(geminiAnalyzer.analyzeData(anyString(), anyString())).thenReturn(geminiResponse);

        // Act
        AnalysisResponse response1 = geminiService.analyzeData(validRequest);
        AnalysisResponse response2 = geminiService.analyzeData(validRequest);

        // Assert - Each response should have a unique ID
        assertNotNull(response1.getId());
        assertNotNull(response2.getId());
        assertNotEquals(response1.getId(), response2.getId());
    }
}
