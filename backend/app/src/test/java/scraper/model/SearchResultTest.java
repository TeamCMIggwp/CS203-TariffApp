package scraper.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for SearchResult with full branch and statement coverage.
 *
 * Coverage Strategy:
 * - STATEMENT COVERAGE: Every line of code is executed at least once
 * - BRANCH COVERAGE: Every decision point (if/else) is tested in both TRUE and FALSE directions
 *
 * Note: SearchResult is a simple data model with no branching logic.
 * This test suite ensures 100% statement coverage by testing all getters/setters.
 */
class SearchResultTest {

    private SearchResult searchResult;

    @BeforeEach
    void setUp() {
        // Create a fresh SearchResult for each test
        searchResult = new SearchResult("https://wto.org/article", "Test Article");
    }

    // ========================================
    // TEST: Constructor
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 10-13
     * Test: Constructor initializes fields correctly
     */
    @Test
    void constructor_withValidParameters_initializesFields() {
        // Arrange & Act
        SearchResult result = new SearchResult("https://example.com", "Example Title");

        // Assert
        assertEquals("https://example.com", result.getUrl());
        assertEquals("Example Title", result.getTitle());
    }

    /**
     * Test: Constructor accepts null values
     */
    @Test
    void constructor_withNullValues_acceptsNulls() {
        // Arrange & Act
        SearchResult result = new SearchResult(null, null);

        // Assert
        assertNull(result.getUrl());
        assertNull(result.getTitle());
    }

    /**
     * Edge case: Constructor with empty strings
     */
    @Test
    void constructor_withEmptyStrings_acceptsEmpty() {
        // Arrange & Act
        SearchResult result = new SearchResult("", "");

        // Assert
        assertEquals("", result.getUrl());
        assertEquals("", result.getTitle());
    }

    // ========================================
    // TEST: getUrl() and setUrl()
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 15-17
     * Test: getUrl() returns initial value from constructor
     */
    @Test
    void getUrl_afterConstruction_returnsConstructorValue() {
        // Assert
        assertEquals("https://wto.org/article", searchResult.getUrl());
    }

    /**
     * STATEMENT COVERAGE: Lines 19-21
     * Test: setUrl() updates the URL
     */
    @Test
    void setUrl_withNewValue_updatesUrl() {
        // Act
        searchResult.setUrl("https://trade.gov/new-article");

        // Assert
        assertEquals("https://trade.gov/new-article", searchResult.getUrl());
    }

    /**
     * Test: setUrl() with null
     */
    @Test
    void setUrl_withNull_acceptsNull() {
        // Act
        searchResult.setUrl(null);

        // Assert
        assertNull(searchResult.getUrl());
    }

    /**
     * Edge case: setUrl() with empty string
     */
    @Test
    void setUrl_withEmptyString_acceptsEmpty() {
        // Act
        searchResult.setUrl("");

        // Assert
        assertEquals("", searchResult.getUrl());
    }

    /**
     * Test: setUrl() multiple times
     */
    @Test
    void setUrl_multipleTimes_retainsLastValue() {
        // Act
        searchResult.setUrl("https://first.com");
        searchResult.setUrl("https://second.com");
        searchResult.setUrl("https://third.com");

        // Assert
        assertEquals("https://third.com", searchResult.getUrl());
    }

    // ========================================
    // TEST: getTitle() and setTitle()
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 23-25
     * Test: getTitle() returns initial value from constructor
     */
    @Test
    void getTitle_afterConstruction_returnsConstructorValue() {
        // Assert
        assertEquals("Test Article", searchResult.getTitle());
    }

    /**
     * STATEMENT COVERAGE: Lines 27-29
     * Test: setTitle() updates the title
     */
    @Test
    void setTitle_withNewValue_updatesTitle() {
        // Act
        searchResult.setTitle("Updated Article Title");

        // Assert
        assertEquals("Updated Article Title", searchResult.getTitle());
    }

    /**
     * Test: setTitle() with null
     */
    @Test
    void setTitle_withNull_acceptsNull() {
        // Act
        searchResult.setTitle(null);

        // Assert
        assertNull(searchResult.getTitle());
    }

    /**
     * Edge case: setTitle() with empty string
     */
    @Test
    void setTitle_withEmptyString_acceptsEmpty() {
        // Act
        searchResult.setTitle("");

        // Assert
        assertEquals("", searchResult.getTitle());
    }

    /**
     * Test: setTitle() with very long string
     */
    @Test
    void setTitle_withLongString_acceptsLongTitle() {
        // Arrange
        String longTitle = "This is a very long title that contains many words and could potentially be a complete sentence describing a comprehensive article about tariff rates and trade agreements between multiple countries";

        // Act
        searchResult.setTitle(longTitle);

        // Assert
        assertEquals(longTitle, searchResult.getTitle());
    }

    /**
     * Test: setTitle() multiple times
     */
    @Test
    void setTitle_multipleTimes_retainsLastValue() {
        // Act
        searchResult.setTitle("First Title");
        searchResult.setTitle("Second Title");
        searchResult.setTitle("Third Title");

        // Assert
        assertEquals("Third Title", searchResult.getTitle());
    }

    // ========================================
    // EDGE CASE TESTS
    // ========================================

    /**
     * Edge case: URL with special characters
     */
    @Test
    void setUrl_withSpecialCharacters_handlesCorrectly() {
        // Arrange
        String specialUrl = "https://example.com/path?query=value&param=test#anchor";

        // Act
        searchResult.setUrl(specialUrl);

        // Assert
        assertEquals(specialUrl, searchResult.getUrl());
    }

    /**
     * Edge case: Title with special characters
     */
    @Test
    void setTitle_withSpecialCharacters_handlesCorrectly() {
        // Arrange
        String specialTitle = "Article: Trade & Tariffs (2024) - 10% Rate";

        // Act
        searchResult.setTitle(specialTitle);

        // Assert
        assertEquals(specialTitle, searchResult.getTitle());
    }

    /**
     * Edge case: Unicode characters in title
     */
    @Test
    void setTitle_withUnicodeCharacters_handlesCorrectly() {
        // Arrange
        String unicodeTitle = "国际贸易关税 - International Trade Tariff";

        // Act
        searchResult.setTitle(unicodeTitle);

        // Assert
        assertEquals(unicodeTitle, searchResult.getTitle());
    }

    /**
     * Integration test: Complete object lifecycle
     */
    @Test
    void searchResult_completeLifecycle_worksCorrectly() {
        // Arrange
        SearchResult result = new SearchResult(null, null);

        // Act & Assert - Set all fields
        result.setUrl("https://wto.org/tariff");
        result.setTitle("WTO Tariff Data");

        assertEquals("https://wto.org/tariff", result.getUrl());
        assertEquals("WTO Tariff Data", result.getTitle());

        // Modify fields
        result.setUrl("https://updated.com/new");
        result.setTitle("Updated Title");

        assertEquals("https://updated.com/new", result.getUrl());
        assertEquals("Updated Title", result.getTitle());
    }
}
