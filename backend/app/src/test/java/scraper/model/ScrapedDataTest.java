package scraper.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for ScrapedData with full branch and statement coverage.
 *
 * Coverage Strategy:
 * - STATEMENT COVERAGE: Every line of code is executed at least once
 * - BRANCH COVERAGE: Every decision point (if/else) is tested in both TRUE and FALSE directions
 *
 * Note: ScrapedData is a simple data model with no branching logic.
 * This test suite ensures 100% statement coverage by testing constructor and all getters/setters.
 */
class ScrapedDataTest {

    private ScrapedData scrapedData;

    @BeforeEach
    void setUp() {
        // Create a fresh ScrapedData for each test
        scrapedData = new ScrapedData("https://wto.org/article", "Test Article");
    }

    // ========================================
    // TEST: Constructor
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 21-25
     * Test: Constructor initializes url, title, and relevantText
     */
    @Test
    void constructor_withValidParameters_initializesFields() {
        // Arrange & Act
        ScrapedData data = new ScrapedData("https://example.com", "Example Title");

        // Assert
        assertEquals("https://example.com", data.getUrl());
        assertEquals("Example Title", data.getTitle());
        assertNotNull(data.getRelevantText());
        assertTrue(data.getRelevantText().isEmpty());
    }

    /**
     * Test: Constructor initializes relevantText as empty ArrayList
     */
    @Test
    void constructor_initializesRelevantTextAsEmptyList() {
        // Arrange & Act
        ScrapedData data = new ScrapedData("url", "title");

        // Assert
        assertNotNull(data.getRelevantText());
        assertEquals(0, data.getRelevantText().size());
        assertTrue(data.getRelevantText() instanceof ArrayList);
    }

    /**
     * Test: Constructor accepts null values for url and title
     */
    @Test
    void constructor_withNullValues_acceptsNulls() {
        // Arrange & Act
        ScrapedData data = new ScrapedData(null, null);

        // Assert
        assertNull(data.getUrl());
        assertNull(data.getTitle());
        assertNotNull(data.getRelevantText()); // relevantText is still initialized
    }

    // ========================================
    // TEST: getUrl() and setUrl()
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 28-30
     * Test: getUrl() returns constructor value
     */
    @Test
    void getUrl_afterConstruction_returnsConstructorValue() {
        // Assert
        assertEquals("https://wto.org/article", scrapedData.getUrl());
    }

    /**
     * STATEMENT COVERAGE: Lines 32-34
     * Test: setUrl() updates the URL
     */
    @Test
    void setUrl_withNewValue_updatesUrl() {
        // Act
        scrapedData.setUrl("https://trade.gov/new");

        // Assert
        assertEquals("https://trade.gov/new", scrapedData.getUrl());
    }

    /**
     * Test: setUrl() with null
     */
    @Test
    void setUrl_withNull_acceptsNull() {
        // Act
        scrapedData.setUrl(null);

        // Assert
        assertNull(scrapedData.getUrl());
    }

    // ========================================
    // TEST: getTitle() and setTitle()
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 36-38
     * Test: getTitle() returns constructor value
     */
    @Test
    void getTitle_afterConstruction_returnsConstructorValue() {
        // Assert
        assertEquals("Test Article", scrapedData.getTitle());
    }

    /**
     * STATEMENT COVERAGE: Lines 40-42
     * Test: setTitle() updates the title
     */
    @Test
    void setTitle_withNewValue_updatesTitle() {
        // Act
        scrapedData.setTitle("Updated Article");

        // Assert
        assertEquals("Updated Article", scrapedData.getTitle());
    }

    /**
     * Test: setTitle() with null
     */
    @Test
    void setTitle_withNull_acceptsNull() {
        // Act
        scrapedData.setTitle(null);

        // Assert
        assertNull(scrapedData.getTitle());
    }

    // ========================================
    // TEST: getSourceDomain() and setSourceDomain()
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 44-46
     * Test: getSourceDomain() returns null initially
     */
    @Test
    void getSourceDomain_afterConstruction_returnsNull() {
        // Assert
        assertNull(scrapedData.getSourceDomain());
    }

    /**
     * STATEMENT COVERAGE: Lines 48-50
     * Test: setSourceDomain() updates the source domain
     */
    @Test
    void setSourceDomain_withValue_updatesSourceDomain() {
        // Act
        scrapedData.setSourceDomain("wto.org");

        // Assert
        assertEquals("wto.org", scrapedData.getSourceDomain());
    }

    /**
     * Test: setSourceDomain() with null
     */
    @Test
    void setSourceDomain_withNull_acceptsNull() {
        // Act
        scrapedData.setSourceDomain("wto.org");
        scrapedData.setSourceDomain(null);

        // Assert
        assertNull(scrapedData.getSourceDomain());
    }

    // ========================================
    // TEST: getRelevantText() and setRelevantText()
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 52-54
     * Test: getRelevantText() returns the list
     */
    @Test
    void getRelevantText_afterConstruction_returnsEmptyList() {
        // Act
        List<String> relevantText = scrapedData.getRelevantText();

        // Assert
        assertNotNull(relevantText);
        assertTrue(relevantText.isEmpty());
    }

    /**
     * STATEMENT COVERAGE: Lines 56-58
     * Test: setRelevantText() replaces the list
     */
    @Test
    void setRelevantText_withNewList_replacesRelevantText() {
        // Arrange
        List<String> newText = Arrays.asList("Text 1", "Text 2", "Text 3");

        // Act
        scrapedData.setRelevantText(newText);

        // Assert
        assertEquals(3, scrapedData.getRelevantText().size());
        assertTrue(scrapedData.getRelevantText().contains("Text 1"));
        assertTrue(scrapedData.getRelevantText().contains("Text 2"));
        assertTrue(scrapedData.getRelevantText().contains("Text 3"));
    }

    /**
     * Test: setRelevantText() with null
     */
    @Test
    void setRelevantText_withNull_acceptsNull() {
        // Act
        scrapedData.setRelevantText(null);

        // Assert
        assertNull(scrapedData.getRelevantText());
    }

    /**
     * Test: Modifying returned list affects the object
     */
    @Test
    void getRelevantText_returnedListIsMutable() {
        // Act
        List<String> relevantText = scrapedData.getRelevantText();
        relevantText.add("New text");

        // Assert
        assertEquals(1, scrapedData.getRelevantText().size());
        assertTrue(scrapedData.getRelevantText().contains("New text"));
    }

    // ========================================
    // TEST: getExporter() and setExporter()
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 60-62
     * Test: getExporter() returns null initially
     */
    @Test
    void getExporter_afterConstruction_returnsNull() {
        // Assert
        assertNull(scrapedData.getExporter());
    }

    /**
     * STATEMENT COVERAGE: Lines 64-66
     * Test: setExporter() updates the exporter
     */
    @Test
    void setExporter_withValue_updatesExporter() {
        // Act
        scrapedData.setExporter("United States");

        // Assert
        assertEquals("United States", scrapedData.getExporter());
    }

    /**
     * Test: setExporter() with null
     */
    @Test
    void setExporter_withNull_acceptsNull() {
        // Act
        scrapedData.setExporter("China");
        scrapedData.setExporter(null);

        // Assert
        assertNull(scrapedData.getExporter());
    }

    // ========================================
    // TEST: getImporter() and setImporter()
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 68-70
     * Test: getImporter() returns null initially
     */
    @Test
    void getImporter_afterConstruction_returnsNull() {
        // Assert
        assertNull(scrapedData.getImporter());
    }

    /**
     * STATEMENT COVERAGE: Lines 72-74
     * Test: setImporter() updates the importer
     */
    @Test
    void setImporter_withValue_updatesImporter() {
        // Act
        scrapedData.setImporter("Japan");

        // Assert
        assertEquals("Japan", scrapedData.getImporter());
    }

    /**
     * Test: setImporter() with null
     */
    @Test
    void setImporter_withNull_acceptsNull() {
        // Act
        scrapedData.setImporter("Singapore");
        scrapedData.setImporter(null);

        // Assert
        assertNull(scrapedData.getImporter());
    }

    // ========================================
    // TEST: getProduct() and setProduct()
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 76-78
     * Test: getProduct() returns null initially
     */
    @Test
    void getProduct_afterConstruction_returnsNull() {
        // Assert
        assertNull(scrapedData.getProduct());
    }

    /**
     * STATEMENT COVERAGE: Lines 80-82
     * Test: setProduct() updates the product
     */
    @Test
    void setProduct_withValue_updatesProduct() {
        // Act
        scrapedData.setProduct("Rice");

        // Assert
        assertEquals("Rice", scrapedData.getProduct());
    }

    /**
     * Test: setProduct() with null
     */
    @Test
    void setProduct_withNull_acceptsNull() {
        // Act
        scrapedData.setProduct("Wheat");
        scrapedData.setProduct(null);

        // Assert
        assertNull(scrapedData.getProduct());
    }

    // ========================================
    // TEST: getYear() and setYear()
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 84-86
     * Test: getYear() returns null initially
     */
    @Test
    void getYear_afterConstruction_returnsNull() {
        // Assert
        assertNull(scrapedData.getYear());
    }

    /**
     * STATEMENT COVERAGE: Lines 88-90
     * Test: setYear() updates the year
     */
    @Test
    void setYear_withValue_updatesYear() {
        // Act
        scrapedData.setYear("2024");

        // Assert
        assertEquals("2024", scrapedData.getYear());
    }

    /**
     * Test: setYear() with null
     */
    @Test
    void setYear_withNull_acceptsNull() {
        // Act
        scrapedData.setYear("2023");
        scrapedData.setYear(null);

        // Assert
        assertNull(scrapedData.getYear());
    }

    // ========================================
    // TEST: getTariffRate() and setTariffRate()
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 92-94
     * Test: getTariffRate() returns null initially
     */
    @Test
    void getTariffRate_afterConstruction_returnsNull() {
        // Assert
        assertNull(scrapedData.getTariffRate());
    }

    /**
     * STATEMENT COVERAGE: Lines 96-98
     * Test: setTariffRate() updates the tariff rate
     */
    @Test
    void setTariffRate_withValue_updatesTariffRate() {
        // Act
        scrapedData.setTariffRate("5.5%");

        // Assert
        assertEquals("5.5%", scrapedData.getTariffRate());
    }

    /**
     * Test: setTariffRate() with null
     */
    @Test
    void setTariffRate_withNull_acceptsNull() {
        // Act
        scrapedData.setTariffRate("10%");
        scrapedData.setTariffRate(null);

        // Assert
        assertNull(scrapedData.getTariffRate());
    }

    // ========================================
    // TEST: getPublishDate() and setPublishDate()
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 100-102
     * Test: getPublishDate() returns null initially
     */
    @Test
    void getPublishDate_afterConstruction_returnsNull() {
        // Assert
        assertNull(scrapedData.getPublishDate());
    }

    /**
     * STATEMENT COVERAGE: Lines 104-106
     * Test: setPublishDate() updates the publish date
     */
    @Test
    void setPublishDate_withValue_updatesPublishDate() {
        // Act
        scrapedData.setPublishDate("2024-01-15");

        // Assert
        assertEquals("2024-01-15", scrapedData.getPublishDate());
    }

    /**
     * Test: setPublishDate() with null
     */
    @Test
    void setPublishDate_withNull_acceptsNull() {
        // Act
        scrapedData.setPublishDate("2023-12-01");
        scrapedData.setPublishDate(null);

        // Assert
        assertNull(scrapedData.getPublishDate());
    }

    // ========================================
    // INTEGRATION TESTS
    // ========================================

    /**
     * Integration test: Complete object lifecycle with all fields
     */
    @Test
    void scrapedData_completeLifecycle_worksCorrectly() {
        // Arrange
        ScrapedData data = new ScrapedData("https://wto.org/tariff", "WTO Tariff Data");

        // Act - Set all fields
        data.setSourceDomain("wto.org");
        data.setExporter("United States");
        data.setImporter("China");
        data.setProduct("Rice");
        data.setYear("2024");
        data.setTariffRate("5.5%");
        data.setPublishDate("2024-01-15");

        List<String> text = new ArrayList<>();
        text.add("Paragraph 1");
        text.add("Paragraph 2");
        data.setRelevantText(text);

        // Assert - All fields are set correctly
        assertEquals("https://wto.org/tariff", data.getUrl());
        assertEquals("WTO Tariff Data", data.getTitle());
        assertEquals("wto.org", data.getSourceDomain());
        assertEquals("United States", data.getExporter());
        assertEquals("China", data.getImporter());
        assertEquals("Rice", data.getProduct());
        assertEquals("2024", data.getYear());
        assertEquals("5.5%", data.getTariffRate());
        assertEquals("2024-01-15", data.getPublishDate());
        assertEquals(2, data.getRelevantText().size());
    }

    /**
     * Edge case: All fields set to null
     */
    @Test
    void scrapedData_allFieldsNull_handlesGracefully() {
        // Arrange & Act
        ScrapedData data = new ScrapedData(null, null);
        data.setSourceDomain(null);
        data.setRelevantText(null);
        data.setExporter(null);
        data.setImporter(null);
        data.setProduct(null);
        data.setYear(null);
        data.setTariffRate(null);
        data.setPublishDate(null);

        // Assert
        assertNull(data.getUrl());
        assertNull(data.getTitle());
        assertNull(data.getSourceDomain());
        assertNull(data.getRelevantText());
        assertNull(data.getExporter());
        assertNull(data.getImporter());
        assertNull(data.getProduct());
        assertNull(data.getYear());
        assertNull(data.getTariffRate());
        assertNull(data.getPublishDate());
    }

    /**
     * Edge case: Multiple updates to same field
     */
    @Test
    void scrapedData_multipleUpdates_retainsLastValue() {
        // Act
        scrapedData.setProduct("Rice");
        scrapedData.setProduct("Wheat");
        scrapedData.setProduct("Corn");

        scrapedData.setYear("2022");
        scrapedData.setYear("2023");
        scrapedData.setYear("2024");

        // Assert
        assertEquals("Corn", scrapedData.getProduct());
        assertEquals("2024", scrapedData.getYear());
    }

    /**
     * Edge case: Empty strings for all string fields
     */
    @Test
    void scrapedData_withEmptyStrings_acceptsEmpty() {
        // Act
        scrapedData.setUrl("");
        scrapedData.setTitle("");
        scrapedData.setSourceDomain("");
        scrapedData.setExporter("");
        scrapedData.setImporter("");
        scrapedData.setProduct("");
        scrapedData.setYear("");
        scrapedData.setTariffRate("");
        scrapedData.setPublishDate("");

        // Assert
        assertEquals("", scrapedData.getUrl());
        assertEquals("", scrapedData.getTitle());
        assertEquals("", scrapedData.getSourceDomain());
        assertEquals("", scrapedData.getExporter());
        assertEquals("", scrapedData.getImporter());
        assertEquals("", scrapedData.getProduct());
        assertEquals("", scrapedData.getYear());
        assertEquals("", scrapedData.getTariffRate());
        assertEquals("", scrapedData.getPublishDate());
    }
}
