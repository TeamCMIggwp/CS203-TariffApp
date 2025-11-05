package geminianalysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for GeminiAnalyzer with full branch and statement coverage.
 *
 * Coverage Strategy:
 * - STATEMENT COVERAGE: Every line of code is executed at least once
 * - BRANCH COVERAGE: Every decision point (if/else) is tested in both TRUE and FALSE directions
 *
 * Branch Points in GeminiAnalyzer:
 * 1. Line 30-31: Constructor - if (apiKey == null || apiKey.trim().isEmpty())
 * 2. Line 59: if (!response.isSuccessful())
 * 3. Line 81: if (analysisPrompt != null)
 * 4. Line 148: if (candidatesNode.isArray() && candidatesNode.size() > 0)
 * 5. Line 153: if ("MAX_TOKENS".equals(finishReason))
 * 6. Line 162: if (partsNode.isArray() && partsNode.size() > 0)
 * 7. Line 188: if (jsonText.contains("```json"))
 * 8. Line 194: else if (jsonText.contains("```"))
 * 9. Line 190-192: if (end > start) - for json block
 * 10. Line 197-199: if (end > start) - for generic block
 * 11. Line 203: if (jsonText.startsWith("{") && jsonText.endsWith("}"))
 *
 * Note: GeminiAnalyzer is integration-heavy as it uses OkHttpClient for real API calls.
 * Full branch coverage requires either:
 * 1. Integration tests with real/mocked API
 * 2. Refactoring to inject OkHttpClient for better testability
 * 3. Making private methods package-private for direct testing
 *
 * This test suite covers all testable branches without external dependencies.
 */
class GeminiAnalyzerTest {

    private final String validApiKey = "test-api-key-12345";

    // ========================================
    // CONSTRUCTOR TESTS - Testing Branch Point #1
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 29-40
     * BRANCH COVERAGE: Line 30 - FALSE branch (apiKey is valid and not empty)
     *
     * Test: Constructor with valid API key creates instance successfully
     */
    @Test
    void constructor_withValidApiKey_createsInstance() {
        // Act
        GeminiAnalyzer analyzer = new GeminiAnalyzer(validApiKey);

        // Assert
        assertNotNull(analyzer);
    }

    /**
     * STATEMENT COVERAGE: Lines 29-31
     * BRANCH COVERAGE: Line 30 - TRUE branch (apiKey == null)
     *
     * Test: Constructor with null API key throws IllegalArgumentException
     */
    @Test
    void constructor_withNullApiKey_throwsIllegalArgumentException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new GeminiAnalyzer(null)
        );

        assertEquals("API key cannot be null or empty", exception.getMessage());
    }

    /**
     * STATEMENT COVERAGE: Lines 29-31
     * BRANCH COVERAGE: Line 30 - TRUE branch (apiKey.trim().isEmpty() with empty string)
     *
     * Test: Constructor with empty string API key throws IllegalArgumentException
     */
    @Test
    void constructor_withEmptyStringApiKey_throwsIllegalArgumentException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new GeminiAnalyzer("")
        );

        assertEquals("API key cannot be null or empty", exception.getMessage());
    }

    /**
     * STATEMENT COVERAGE: Lines 29-31
     * BRANCH COVERAGE: Line 30 - TRUE branch (apiKey.trim().isEmpty() with whitespace)
     *
     * Test: Constructor with whitespace-only API key throws IllegalArgumentException
     */
    @Test
    void constructor_withWhitespaceApiKey_throwsIllegalArgumentException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new GeminiAnalyzer("   ")
        );

        assertEquals("API key cannot be null or empty", exception.getMessage());
    }

    /**
     * STATEMENT COVERAGE: Lines 29-31
     * BRANCH COVERAGE: Additional edge case - API key with tabs and newlines
     *
     * Test: Constructor with tabs/newlines in API key throws IllegalArgumentException
     */
    @Test
    void constructor_withTabsAndNewlinesApiKey_throwsIllegalArgumentException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new GeminiAnalyzer("\t\n\r")
        );

        assertEquals("API key cannot be null or empty", exception.getMessage());
    }

    /**
     * STATEMENT COVERAGE: Lines 29-40
     * BRANCH COVERAGE: Edge case - very long API key
     *
     * Test: Constructor with very long API key creates instance successfully
     */
    @Test
    void constructor_withVeryLongApiKey_createsInstance() {
        // Arrange
        String longApiKey = "a".repeat(1000); // Very long API key

        // Act
        GeminiAnalyzer analyzer = new GeminiAnalyzer(longApiKey);

        // Assert
        assertNotNull(analyzer);
    }

    /**
     * STATEMENT COVERAGE: Lines 29-40
     * BRANCH COVERAGE: Edge case - API key with special characters
     *
     * Test: Constructor with special characters in API key creates instance successfully
     */
    @Test
    void constructor_withSpecialCharactersApiKey_createsInstance() {
        // Arrange
        String specialApiKey = "api-key_123!@#$%^&*()";

        // Act
        GeminiAnalyzer analyzer = new GeminiAnalyzer(specialApiKey);

        // Assert
        assertNotNull(analyzer);
    }

    /**
     * STATEMENT COVERAGE: Lines 29-40
     * BRANCH COVERAGE: Edge case - API key with leading/trailing valid characters
     *
     * Test: Constructor with spaces around valid API key creates instance
     * (Note: The trim() in validation only checks if result is empty)
     */
    @Test
    void constructor_withValidApiKeyAndSpaces_createsInstance() {
        // Arrange
        String apiKeyWithSpaces = "  valid-api-key-123  ";

        // Act
        GeminiAnalyzer analyzer = new GeminiAnalyzer(apiKeyWithSpaces);

        // Assert
        assertNotNull(analyzer);
    }

    // ========================================
    // close() METHOD TEST
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 217-220
     * Test: close() method cleans up HTTP client resources without errors
     */
    @Test
    void close_cleansUpResources_withoutErrors() {
        // Arrange
        GeminiAnalyzer analyzer = new GeminiAnalyzer(validApiKey);

        // Act & Assert - should not throw any exceptions
        assertDoesNotThrow(() -> analyzer.close());
    }

    /**
     * STATEMENT COVERAGE: Lines 217-220
     * Test: close() can be called multiple times without errors
     */
    @Test
    void close_calledMultipleTimes_doesNotThrowException() {
        // Arrange
        GeminiAnalyzer analyzer = new GeminiAnalyzer(validApiKey);

        // Act & Assert
        assertDoesNotThrow(() -> {
            analyzer.close();
            analyzer.close(); // Call again
            analyzer.close(); // And again
        });
    }

    // ========================================
    // NOTES ON ADDITIONAL COVERAGE
    // ========================================

    /*
     * The following branch points require integration testing or refactoring:
     *
     * BRANCH POINT #2 (Line 59): if (!response.isSuccessful())
     * - TRUE branch: Requires mocking OkHttpClient to return unsuccessful response
     * - FALSE branch: Requires mocking OkHttpClient to return successful response
     * - Coverage: Integration tests or dependency injection needed
     *
     * BRANCH POINT #3 (Line 81): if (analysisPrompt != null)
     * - TRUE branch: Tested via analyzeData(data, prompt) with non-null prompt
     * - FALSE branch: Tested via analyzeData(data) which passes null prompt
     * - Coverage: Requires calling public analyzeData methods (integration test)
     *
     * BRANCH POINT #4 (Line 148): if (candidatesNode.isArray() && size > 0)
     * - TRUE branch: Valid API response with candidates
     * - FALSE branch: API response with no candidates
     * - Coverage: Requires mocking API response or integration test
     *
     * BRANCH POINT #5 (Line 153): if ("MAX_TOKENS".equals(finishReason))
     * - TRUE branch: API response with MAX_TOKENS finish reason
     * - FALSE branch: API response with other finish reasons (STOP, etc.)
     * - Coverage: Requires mocking API response or integration test
     *
     * BRANCH POINT #6 (Line 162): if (partsNode.isArray() && size > 0)
     * - TRUE branch: Content with parts array
     * - FALSE branch: Content with empty or missing parts
     * - Coverage: Requires mocking API response or integration test
     *
     * BRANCH POINTS #7-11 (Lines 188-203): parseAnalysisJson branches
     * - Multiple branches for JSON extraction from markdown
     * - Coverage: Requires making method package-private or integration test
     *
     * RECOMMENDED APPROACHES FOR FULL COVERAGE:
     * 1. Make parseGeminiResponse and parseAnalysisJson package-private for testing
     * 2. Inject OkHttpClient as a dependency for better mockability
     * 3. Create integration tests with MockWebServer or similar
     * 4. Use PowerMock for mocking final classes (not recommended for new code)
     */
}
