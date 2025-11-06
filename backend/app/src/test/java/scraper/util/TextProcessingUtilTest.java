package scraper.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for TextProcessingUtil with full branch and statement coverage.
 *
 * Coverage Strategy:
 * - STATEMENT COVERAGE: Every line of code is executed at least once
 * - BRANCH COVERAGE: Every decision point (if/else) is tested in both TRUE and FALSE directions
 *
 * Branch Points in TextProcessingUtil:
 * 1. Line 24: if (text == null)
 * 2. Line 29-33: OR conditions for tariff keywords
 * 3. Line 40: if (text == null)
 * 4. Line 46: if (matcher.find())
 * 5. Line 52: if (matcher.find())
 * 6. Line 63: if (text == null)
 */
class TextProcessingUtilTest {

    private TextProcessingUtil textProcessor;

    @BeforeEach
    void setUp() {
        textProcessor = new TextProcessingUtil();
    }

    // ========================================
    // TEST: containsTariffKeywords() - ALL BRANCHES
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 23-34
     * BRANCH COVERAGE: Line 24 - TRUE (text == null)
     *
     * Test: Null text returns false
     */
    @Test
    void containsTariffKeywords_withNull_returnsFalse() {
        // Act
        boolean result = textProcessor.containsTariffKeywords(null);

        // Assert
        assertFalse(result);
    }

    /**
     * STATEMENT COVERAGE: Lines 23-34
     * BRANCH COVERAGE: Line 29 - TRUE (contains "tariff")
     *
     * Test: Text with "tariff" returns true
     */
    @Test
    void containsTariffKeywords_withTariff_returnsTrue() {
        // Act
        boolean result = textProcessor.containsTariffKeywords("The tariff rate is 5%");

        // Assert
        assertTrue(result);
    }

    /**
     * STATEMENT COVERAGE: Lines 23-34
     * BRANCH COVERAGE: Line 30 - TRUE (contains "duty rate")
     *
     * Test: Text with "duty rate" returns true
     */
    @Test
    void containsTariffKeywords_withDutyRate_returnsTrue() {
        // Act
        boolean result = textProcessor.containsTariffKeywords("The duty rate for imports is 10%");

        // Assert
        assertTrue(result);
    }

    /**
     * STATEMENT COVERAGE: Lines 23-34
     * BRANCH COVERAGE: Line 31 - TRUE (contains "customs duty")
     *
     * Test: Text with "customs duty" returns true
     */
    @Test
    void containsTariffKeywords_withCustomsDuty_returnsTrue() {
        // Act
        boolean result = textProcessor.containsTariffKeywords("Customs duty applies to all goods");

        // Assert
        assertTrue(result);
    }

    /**
     * STATEMENT COVERAGE: Lines 23-34
     * BRANCH COVERAGE: Line 32 - TRUE (contains "import duty")
     *
     * Test: Text with "import duty" returns true
     */
    @Test
    void containsTariffKeywords_withImportDuty_returnsTrue() {
        // Act
        boolean result = textProcessor.containsTariffKeywords("Import duty is calculated monthly");

        // Assert
        assertTrue(result);
    }

    /**
     * STATEMENT COVERAGE: Lines 23-34
     * BRANCH COVERAGE: Line 33 - TRUE (contains "rate" AND "percent")
     *
     * Test: Text with "rate" and "percent" returns true
     */
    @Test
    void containsTariffKeywords_withRateAndPercent_returnsTrue() {
        // Act
        boolean result = textProcessor.containsTariffKeywords("The rate is 15 percent");

        // Assert
        assertTrue(result);
    }

    /**
     * STATEMENT COVERAGE: Lines 23-34
     * BRANCH COVERAGE: Line 33 - TRUE (contains "rate" AND "%")
     *
     * Test: Text with "rate" and "%" returns true
     */
    @Test
    void containsTariffKeywords_withRateAndPercentSymbol_returnsTrue() {
        // Act
        boolean result = textProcessor.containsTariffKeywords("The rate is 15%");

        // Assert
        assertTrue(result);
    }

    /**
     * STATEMENT COVERAGE: Lines 23-34
     * BRANCH COVERAGE: All conditions FALSE
     *
     * Test: Text without keywords returns false
     */
    @Test
    void containsTariffKeywords_withNoKeywords_returnsFalse() {
        // Act
        boolean result = textProcessor.containsTariffKeywords("This is just regular text about trade");

        // Assert
        assertFalse(result);
    }

    /**
     * Edge case: Case insensitivity
     */
    @Test
    void containsTariffKeywords_isCaseInsensitive() {
        // Act & Assert
        assertTrue(textProcessor.containsTariffKeywords("TARIFF"));
        assertTrue(textProcessor.containsTariffKeywords("Tariff"));
        assertTrue(textProcessor.containsTariffKeywords("TaRiFf"));
        assertTrue(textProcessor.containsTariffKeywords("DUTY RATE"));
    }

    /**
     * Edge case: Empty string
     */
    @Test
    void containsTariffKeywords_withEmptyString_returnsFalse() {
        // Act
        boolean result = textProcessor.containsTariffKeywords("");

        // Assert
        assertFalse(result);
    }

    // ========================================
    // TEST: extractYearFromText() - ALL BRANCHES
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 39-57
     * BRANCH COVERAGE: Line 40 - TRUE (text == null)
     *
     * Test: Null text returns null
     */
    @Test
    void extractYearFromText_withNull_returnsNull() {
        // Act
        String result = textProcessor.extractYearFromText(null);

        // Assert
        assertNull(result);
    }

    /**
     * STATEMENT COVERAGE: Lines 39-57
     * BRANCH COVERAGE: Line 46 - TRUE (contextual pattern found)
     *
     * Test: Contextual year pattern is extracted
     */
    @Test
    void extractYearFromText_withContextualYear_extractsCorrectly() {
        // Act & Assert
        assertEquals("2023", textProcessor.extractYearFromText("Published: 2023"));
        assertEquals("2024", textProcessor.extractYearFromText("Updated: 2024"));
        assertEquals("2022", textProcessor.extractYearFromText("Effective: 2022"));
        assertEquals("2021", textProcessor.extractYearFromText("Dated: 2021"));
        assertEquals("2020", textProcessor.extractYearFromText("As of: 2020"));
        assertEquals("2023", textProcessor.extractYearFromText("© 2023 Company"));
        assertEquals("2023", textProcessor.extractYearFromText("Copyright 2023"));
    }

    /**
     * STATEMENT COVERAGE: Lines 39-57
     * BRANCH COVERAGE: Line 46 - FALSE, Line 52 - TRUE (generic pattern found)
     *
     * Test: Generic year pattern is extracted as fallback
     */
    @Test
    void extractYearFromText_withGenericYear_extractsCorrectly() {
        // Act & Assert
        assertEquals("2023", textProcessor.extractYearFromText("Document from 2023"));
        assertEquals("2024", textProcessor.extractYearFromText("The year 2024 was significant"));
        assertEquals("2020", textProcessor.extractYearFromText("2020 statistics"));
        assertEquals("2029", textProcessor.extractYearFromText("Projected until 2029"));
    }

    /**
     * STATEMENT COVERAGE: Lines 39-57
     * BRANCH COVERAGE: Line 46 - FALSE, Line 52 - FALSE (no year found)
     *
     * Test: No year in text returns null
     */
    @Test
    void extractYearFromText_withNoYear_returnsNull() {
        // Act
        String result = textProcessor.extractYearFromText("Text without any year information");

        // Assert
        assertNull(result);
    }

    /**
     * Edge case: Multiple years present (should return first match)
     */
    @Test
    void extractYearFromText_withMultipleYears_returnsFirst() {
        // Act
        String result = textProcessor.extractYearFromText("Published: 2023, Updated: 2024");

        // Assert
        assertEquals("2023", result); // First contextual match
    }

    /**
     * Edge case: Year outside 2020-2029 range is not matched by generic pattern
     */
    @Test
    void extractYearFromText_withOldYear_returnsNull() {
        // Act - 2019 is not in 202X range
        String result = textProcessor.extractYearFromText("Document from 2019");

        // Assert - Should not match generic pattern (only matches 2020-2029)
        assertNull(result);
    }

    /**
     * Edge case: Empty string
     */
    @Test
    void extractYearFromText_withEmptyString_returnsNull() {
        // Act
        String result = textProcessor.extractYearFromText("");

        // Assert
        assertNull(result);
    }

    // ========================================
    // TEST: cleanText() - ALL BRANCHES
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 62-68
     * BRANCH COVERAGE: Line 63 - TRUE (text == null)
     *
     * Test: Null text returns empty string
     */
    @Test
    void cleanText_withNull_returnsEmptyString() {
        // Act
        String result = textProcessor.cleanText(null);

        // Assert
        assertEquals("", result);
    }

    /**
     * STATEMENT COVERAGE: Lines 62-68
     * BRANCH COVERAGE: Line 63 - FALSE (text not null)
     *
     * Test: Multiple spaces are replaced with single space
     */
    @Test
    void cleanText_removesExtraWhitespace() {
        // Act
        String result = textProcessor.cleanText("Text  with   multiple    spaces");

        // Assert
        assertEquals("Text with multiple spaces", result);
    }

    /**
     * Test: Tabs and newlines are normalized to single space
     */
    @Test
    void cleanText_normalizesTabsAndNewlines() {
        // Act
        String result = textProcessor.cleanText("Text\twith\ttabs\nand\nnewlines");

        // Assert
        assertEquals("Text with tabs and newlines", result);
    }

    /**
     * Test: Leading and trailing whitespace is trimmed
     */
    @Test
    void cleanText_trimsLeadingAndTrailingWhitespace() {
        // Act
        String result = textProcessor.cleanText("   Text with spaces   ");

        // Assert
        assertEquals("Text with spaces", result);
    }

    /**
     * Edge case: Empty string
     */
    @Test
    void cleanText_withEmptyString_returnsEmptyString() {
        // Act
        String result = textProcessor.cleanText("");

        // Assert
        assertEquals("", result);
    }

    /**
     * Edge case: Only whitespace
     */
    @Test
    void cleanText_withOnlyWhitespace_returnsEmptyString() {
        // Act
        String result = textProcessor.cleanText("     \t\n   ");

        // Assert
        assertEquals("", result);
    }

    /**
     * Edge case: Already clean text
     */
    @Test
    void cleanText_withAlreadyCleanText_returnsUnchanged() {
        // Act
        String result = textProcessor.cleanText("Clean text");

        // Assert
        assertEquals("Clean text", result);
    }
}
