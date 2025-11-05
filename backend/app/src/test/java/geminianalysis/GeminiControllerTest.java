package geminianalysis;

import geminianalysis.dto.AnalysisRequest;
import geminianalysis.dto.AnalysisResponse;
import geminianalysis.service.GeminiService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for GeminiController with full branch and statement coverage.
 *
 * Coverage Strategy:
 * - STATEMENT COVERAGE: Every line of code is executed at least once
 * - BRANCH COVERAGE: Every decision point (if/else) is tested in both TRUE and FALSE directions
 */
@ExtendWith(MockitoExtension.class)
class GeminiControllerTest {

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private GeminiController geminiController;

    private AnalysisRequest validRequest;
    private AnalysisResponse successResponse;
    private AnalysisResponse errorResponse;

    @BeforeEach
    void setUp() {
        // Set up valid request
        validRequest = new AnalysisRequest();
        validRequest.setData("Test data for analysis");
        validRequest.setPrompt("Analyze this data");

        // Set up success response
        successResponse = new AnalysisResponse(
            true,
            "structured",
            Map.of("summary", "Test summary", "confidence", "high"),
            "Test summary",
            "high"
        );
        successResponse.setId("test-id-123");

        // Set up error response
        errorResponse = new AnalysisResponse(false, "API error occurred");

        // Set API key for health check tests
        ReflectionTestUtils.setField(geminiController, "apiKey", "test-api-key");
    }

    // ========================================
    // TEST: createAnalysis() - SUCCESS PATH
    // ========================================

    /**
     * STATEMENT COVERAGE: Executes lines 125-127, 129-130
     * BRANCH COVERAGE: Tests TRUE branch of if(response.isSuccess()) on line 129
     */
    @Test
    void createAnalysis_withValidRequest_returnsOkResponse() {
        // Arrange
        when(geminiService.analyzeData(any(AnalysisRequest.class))).thenReturn(successResponse);

        // Act
        ResponseEntity<AnalysisResponse> response = geminiController.createAnalysis(validRequest);

        // Assert - Statement Coverage
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("test-id-123", response.getBody().getId());
        assertEquals("structured", response.getBody().getAnalysisType());

        // Verify service interaction
        verify(geminiService, times(1)).analyzeData(validRequest);
    }

    // ========================================
    // TEST: createAnalysis() - ERROR PATH
    // ========================================

    /**
     * STATEMENT COVERAGE: Executes lines 125-127, 129, 132
     * BRANCH COVERAGE: Tests FALSE branch of if(response.isSuccess()) on line 129
     */
    @Test
    void createAnalysis_withServiceError_returnsInternalServerError() {
        // Arrange
        when(geminiService.analyzeData(any(AnalysisRequest.class))).thenReturn(errorResponse);

        // Act
        ResponseEntity<AnalysisResponse> response = geminiController.createAnalysis(validRequest);

        // Assert - Statement Coverage
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("API error occurred", response.getBody().getError());

        // Verify service interaction
        verify(geminiService, times(1)).analyzeData(validRequest);
    }

    /**
     * STATEMENT COVERAGE: Tests logging on line 125
     * BRANCH COVERAGE: Additional test for success path with different data
     */
    @Test
    void createAnalysis_withDifferentData_logsCorrectly() {
        // Arrange
        AnalysisRequest differentRequest = new AnalysisRequest();
        differentRequest.setData("Different test data");
        differentRequest.setPrompt("Different prompt");

        when(geminiService.analyzeData(any(AnalysisRequest.class))).thenReturn(successResponse);

        // Act
        ResponseEntity<AnalysisResponse> response = geminiController.createAnalysis(differentRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(geminiService, times(1)).analyzeData(differentRequest);
    }

    // ========================================
    // TEST: health() - API KEY CONFIGURED
    // ========================================

    /**
     * STATEMENT COVERAGE: Executes lines 164-169
     * BRANCH COVERAGE: Tests TRUE branch of (apiKey != null && !apiKey.trim().isEmpty()) on line 168
     */
    @Test
    void health_withApiKeyConfigured_returnsHealthyStatus() {
        // Arrange - API key already set in setUp()

        // Act
        ResponseEntity<Map<String, Object>> response = geminiController.health();

        // Assert - Statement Coverage
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> health = response.getBody();
        assertEquals("UP", health.get("status"));
        assertEquals("gemini-analysis", health.get("service"));
        assertTrue(health.containsKey("timestamp"));
        assertTrue((Boolean) health.get("apiKeyConfigured"));

        // Verify timestamp is a valid number
        assertTrue(health.get("timestamp") instanceof Long);
        assertTrue((Long) health.get("timestamp") > 0);
    }

    // ========================================
    // TEST: health() - API KEY NULL
    // ========================================

    /**
     * STATEMENT COVERAGE: Executes lines 164-169
     * BRANCH COVERAGE: Tests FALSE branch (apiKey == null) of line 168
     */
    @Test
    void health_withNullApiKey_returnsApiKeyNotConfigured() {
        // Arrange
        ReflectionTestUtils.setField(geminiController, "apiKey", null);

        // Act
        ResponseEntity<Map<String, Object>> response = geminiController.health();

        // Assert - Branch Coverage for NULL case
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> health = response.getBody();
        assertFalse((Boolean) health.get("apiKeyConfigured"));
    }

    // ========================================
    // TEST: health() - API KEY EMPTY STRING
    // ========================================

    /**
     * STATEMENT COVERAGE: Executes lines 164-169
     * BRANCH COVERAGE: Tests FALSE branch (apiKey.trim().isEmpty()) of line 168
     */
    @Test
    void health_withEmptyApiKey_returnsApiKeyNotConfigured() {
        // Arrange
        ReflectionTestUtils.setField(geminiController, "apiKey", "");

        // Act
        ResponseEntity<Map<String, Object>> response = geminiController.health();

        // Assert - Branch Coverage for empty string
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> health = response.getBody();
        assertFalse((Boolean) health.get("apiKeyConfigured"));
    }

    // ========================================
    // TEST: health() - API KEY WHITESPACE
    // ========================================

    /**
     * STATEMENT COVERAGE: Executes lines 164-169
     * BRANCH COVERAGE: Tests FALSE branch (apiKey.trim().isEmpty()) for whitespace
     */
    @Test
    void health_withWhitespaceApiKey_returnsApiKeyNotConfigured() {
        // Arrange
        ReflectionTestUtils.setField(geminiController, "apiKey", "   ");

        // Act
        ResponseEntity<Map<String, Object>> response = geminiController.health();

        // Assert - Branch Coverage for whitespace string
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> health = response.getBody();
        assertFalse((Boolean) health.get("apiKeyConfigured"));
    }

    // ========================================
    // EDGE CASE TESTS
    // ========================================

    /**
     * Edge case: Request with null prompt (should still work)
     */
    @Test
    void createAnalysis_withNullPrompt_succeeds() {
        // Arrange
        AnalysisRequest requestWithNullPrompt = new AnalysisRequest();
        requestWithNullPrompt.setData("Test data");
        requestWithNullPrompt.setPrompt(null);

        when(geminiService.analyzeData(any(AnalysisRequest.class))).thenReturn(successResponse);

        // Act
        ResponseEntity<AnalysisResponse> response = geminiController.createAnalysis(requestWithNullPrompt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(geminiService, times(1)).analyzeData(requestWithNullPrompt);
    }

    /**
     * Edge case: Response with text analysis type instead of structured
     */
    @Test
    void createAnalysis_withTextAnalysis_returnsOk() {
        // Arrange
        AnalysisResponse textResponse = new AnalysisResponse(
            true,
            "text",
            "Plain text analysis result",
            "Text summary",
            "medium"
        );

        when(geminiService.analyzeData(any(AnalysisRequest.class))).thenReturn(textResponse);

        // Act
        ResponseEntity<AnalysisResponse> response = geminiController.createAnalysis(validRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("text", response.getBody().getAnalysisType());
        assertEquals("Plain text analysis result", response.getBody().getAnalysis());
    }

    /**
     * Edge case: Response without ID set (service doesn't set it)
     */
    @Test
    void createAnalysis_withoutIdInResponse_stillSucceeds() {
        // Arrange
        AnalysisResponse responseWithoutId = new AnalysisResponse(
            true,
            "structured",
            Map.of("data", "value"),
            "Summary",
            "high"
        );
        // Note: ID not set

        when(geminiService.analyzeData(any(AnalysisRequest.class))).thenReturn(responseWithoutId);

        // Act
        ResponseEntity<AnalysisResponse> response = geminiController.createAnalysis(validRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody().getId());
    }
}
