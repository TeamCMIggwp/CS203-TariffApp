package scraper.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for UrlUtil with full branch and statement coverage.
 *
 * Coverage Strategy:
 * - STATEMENT COVERAGE: Every line of code is executed at least once
 * - BRANCH COVERAGE: Every decision point (if/else) is tested in both TRUE and FALSE directions
 *
 * Branch Points in UrlUtil:
 * 1. Line 43: if (url == null)
 * 2. Line 57: if (url.contains(domain)) - loop for each trusted source
 * 3. Line 64-66: try-catch for URL parsing
 * 4. Line 81: if (url == null)
 * 5. Line 85-86: stream().anyMatch() - TRUE and FALSE paths
 */
class UrlUtilTest {

    private UrlUtil urlUtil;

    @BeforeEach
    void setUp() {
        urlUtil = new UrlUtil();
    }

    // ========================================
    // TEST: fixEncoding() - ALL BRANCHES
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 42-49
     * BRANCH COVERAGE: Line 43 - TRUE (url == null)
     *
     * Test: Null URL returns null
     */
    @Test
    void fixEncoding_withNull_returnsNull() {
        // Act
        String result = urlUtil.fixEncoding(null);

        // Assert
        assertNull(result);
    }

    /**
     * STATEMENT COVERAGE: Lines 42-49
     * BRANCH COVERAGE: Line 43 - FALSE (url not null)
     *
     * Test: Pipe character is encoded to %7C
     */
    @Test
    void fixEncoding_withPipe_encodesToPercent7C() {
        // Arrange
        String urlWithPipe = "https://example.com/path|with|pipes";

        // Act
        String result = urlUtil.fixEncoding(urlWithPipe);

        // Assert
        assertEquals("https://example.com/path%7Cwith%7Cpipes", result);
    }

    /**
     * STATEMENT COVERAGE: Lines 42-49
     * Test: Space is encoded to %20
     */
    @Test
    void fixEncoding_withSpace_encodesToPercent20() {
        // Arrange
        String urlWithSpace = "https://example.com/path with spaces";

        // Act
        String result = urlUtil.fixEncoding(urlWithSpace);

        // Assert
        assertEquals("https://example.com/path%20with%20spaces", result);
    }

    /**
     * Test: Both pipe and space are encoded together
     */
    @Test
    void fixEncoding_withPipeAndSpace_encodesBoth() {
        // Arrange
        String urlWithBoth = "https://example.com/path|with spaces";

        // Act
        String result = urlUtil.fixEncoding(urlWithBoth);

        // Assert
        assertEquals("https://example.com/path%7Cwith%20spaces", result);
    }

    /**
     * Edge case: Already clean URL remains unchanged
     */
    @Test
    void fixEncoding_withCleanUrl_returnsUnchanged() {
        // Arrange
        String cleanUrl = "https://example.com/clean/path";

        // Act
        String result = urlUtil.fixEncoding(cleanUrl);

        // Assert
        assertEquals("https://example.com/clean/path", result);
    }

    /**
     * Edge case: Empty string returns empty string
     */
    @Test
    void fixEncoding_withEmptyString_returnsEmpty() {
        // Act
        String result = urlUtil.fixEncoding("");

        // Assert
        assertEquals("", result);
    }

    // ========================================
    // TEST: extractDomain() - ALL BRANCHES
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 54-68
     * BRANCH COVERAGE: Line 57 - TRUE (contains trusted source)
     *
     * Test: WTO.org URL returns trusted domain
     */
    @Test
    void extractDomain_withWtoUrl_returnsTrustedDomain() {
        // Act
        String result = urlUtil.extractDomain("https://www.wto.org/english/tratop_e/tariffs_e/tariffs_e.htm");

        // Assert
        assertEquals("wto.org", result);
    }

    /**
     * Test: All trusted sources are correctly identified
     */
    @Test
    void extractDomain_withAllTrustedSources_returnsCorrectDomains() {
        // Assert all trusted domains
        assertEquals("wto.org", urlUtil.extractDomain("https://www.wto.org/article"));
        assertEquals("trade.gov", urlUtil.extractDomain("https://www.trade.gov/data"));
        assertEquals("usitc.gov", urlUtil.extractDomain("https://www.usitc.gov/tariff"));
        assertEquals("cbp.gov", urlUtil.extractDomain("https://www.cbp.gov/customs"));
        assertEquals("worldbank.org", urlUtil.extractDomain("https://www.worldbank.org/data"));
        assertEquals("comtrade.un.org", urlUtil.extractDomain("https://comtrade.un.org/api"));
        assertEquals("oecd.org", urlUtil.extractDomain("https://www.oecd.org/trade"));
        assertEquals("export.gov", urlUtil.extractDomain("https://www.export.gov/tariff"));
        assertEquals("trade-tariff.service.gov.uk", urlUtil.extractDomain("https://www.trade-tariff.service.gov.uk/"));
    }

    /**
     * STATEMENT COVERAGE: Lines 62-67
     * BRANCH COVERAGE: Line 57 - FALSE, try block - SUCCESS
     *
     * Test: Non-trusted source extracts host from URL
     */
    @Test
    void extractDomain_withNonTrustedSource_extractsHost() {
        // Act
        String result = urlUtil.extractDomain("https://www.example.com/some/path");

        // Assert
        assertEquals("www.example.com", result);
    }

    /**
     * Test: Complex non-trusted URL extracts host correctly
     */
    @Test
    void extractDomain_withComplexUrl_extractsHost() {
        // Act
        String result = urlUtil.extractDomain("https://subdomain.example.com:8080/path?query=value");

        // Assert
        assertEquals("subdomain.example.com", result);
    }

    /**
     * STATEMENT COVERAGE: Lines 65-66
     * BRANCH COVERAGE: catch block - TRUE (exception thrown)
     *
     * Test: Invalid URL returns "unknown"
     */
    @Test
    void extractDomain_withInvalidUrl_returnsUnknown() {
        // Act
        String result = urlUtil.extractDomain("not a valid url at all");

        // Assert
        assertEquals("unknown", result);
    }

    /**
     * Test: Malformed URL with special characters returns "unknown"
     */
    @Test
    void extractDomain_withMalformedUrl_returnsUnknown() {
        // Act
        String result = urlUtil.extractDomain("://missing-protocol.com");

        // Assert
        assertEquals("unknown", result);
    }

    /**
     * Edge case: HTTP (not HTTPS) URL works correctly
     */
    @Test
    void extractDomain_withHttpUrl_extractsHost() {
        // Act
        String result = urlUtil.extractDomain("http://example.com/path");

        // Assert
        assertEquals("example.com", result);
    }

    // ========================================
    // TEST: getRandomUserAgent() - ALL BRANCHES
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 73-75
     * Test: Returns one of the valid user agents
     */
    @Test
    void getRandomUserAgent_returnsValidUserAgent() {
        // Act
        String userAgent = urlUtil.getRandomUserAgent();

        // Assert - Should be one of the 4 predefined user agents
        assertNotNull(userAgent);
        assertTrue(
            userAgent.contains("Mozilla/5.0") &&
            (userAgent.contains("Chrome/120.0.0.0") ||
             userAgent.contains("Edge/120.0.0.0") ||
             userAgent.contains("Safari/605.1.15"))
        );
    }

    /**
     * Test: Multiple calls return user agents (may be different due to randomness)
     */
    @Test
    void getRandomUserAgent_multipleCallsReturnUserAgents() {
        // Act - Call multiple times
        String ua1 = urlUtil.getRandomUserAgent();
        String ua2 = urlUtil.getRandomUserAgent();
        String ua3 = urlUtil.getRandomUserAgent();

        // Assert - All should be valid
        assertNotNull(ua1);
        assertNotNull(ua2);
        assertNotNull(ua3);

        // All should start with Mozilla/5.0
        assertTrue(ua1.startsWith("Mozilla/5.0"));
        assertTrue(ua2.startsWith("Mozilla/5.0"));
        assertTrue(ua3.startsWith("Mozilla/5.0"));
    }

    /**
     * Test: User agent is long enough to be realistic
     */
    @Test
    void getRandomUserAgent_hasRealisticLength() {
        // Act
        String userAgent = urlUtil.getRandomUserAgent();

        // Assert - User agents should be at least 100 characters
        assertTrue(userAgent.length() > 100);
    }

    // ========================================
    // TEST: isTrustedSource() - ALL BRANCHES
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 80-87
     * BRANCH COVERAGE: Line 81 - TRUE (url == null)
     *
     * Test: Null URL returns false
     */
    @Test
    void isTrustedSource_withNull_returnsFalse() {
        // Act
        boolean result = urlUtil.isTrustedSource(null);

        // Assert
        assertFalse(result);
    }

    /**
     * STATEMENT COVERAGE: Lines 85-86
     * BRANCH COVERAGE: anyMatch() - TRUE (trusted source found)
     *
     * Test: WTO URL is identified as trusted
     */
    @Test
    void isTrustedSource_withWtoUrl_returnsTrue() {
        // Act
        boolean result = urlUtil.isTrustedSource("https://www.wto.org/tariff/data");

        // Assert
        assertTrue(result);
    }

    /**
     * Test: All trusted sources are identified correctly
     */
    @Test
    void isTrustedSource_withAllTrustedDomains_returnsTrue() {
        // Assert - All trusted sources
        assertTrue(urlUtil.isTrustedSource("https://www.wto.org/article"));
        assertTrue(urlUtil.isTrustedSource("https://www.trade.gov/data"));
        assertTrue(urlUtil.isTrustedSource("https://www.usitc.gov/tariff"));
        assertTrue(urlUtil.isTrustedSource("https://www.cbp.gov/customs"));
        assertTrue(urlUtil.isTrustedSource("https://www.worldbank.org/data"));
        assertTrue(urlUtil.isTrustedSource("https://comtrade.un.org/api"));
        assertTrue(urlUtil.isTrustedSource("https://www.oecd.org/trade"));
        assertTrue(urlUtil.isTrustedSource("https://www.export.gov/tariff"));
        assertTrue(urlUtil.isTrustedSource("https://www.trade-tariff.service.gov.uk/"));
    }

    /**
     * STATEMENT COVERAGE: Lines 85-86
     * BRANCH COVERAGE: anyMatch() - FALSE (no trusted source)
     *
     * Test: Non-trusted URL returns false
     */
    @Test
    void isTrustedSource_withNonTrustedUrl_returnsFalse() {
        // Act
        boolean result = urlUtil.isTrustedSource("https://www.example.com/tariff");

        // Assert
        assertFalse(result);
    }

    /**
     * Test: Various non-trusted sources return false
     */
    @Test
    void isTrustedSource_withVariousNonTrustedUrls_returnsFalse() {
        // Assert - Non-trusted sources
        assertFalse(urlUtil.isTrustedSource("https://www.random-blog.com/tariff"));
        assertFalse(urlUtil.isTrustedSource("https://news.example.org/trade"));
        assertFalse(urlUtil.isTrustedSource("https://untrusted-source.net/data"));
    }

    /**
     * Edge case: Empty string returns false
     */
    @Test
    void isTrustedSource_withEmptyString_returnsFalse() {
        // Act
        boolean result = urlUtil.isTrustedSource("");

        // Assert
        assertFalse(result);
    }

    /**
     * Edge case: Partial domain match (e.g., "wto.org" substring)
     */
    @Test
    void isTrustedSource_withPartialMatch_returnsTrue() {
        // Act - URL contains "wto.org" as substring
        boolean result = urlUtil.isTrustedSource("https://subdomain.wto.org/path");

        // Assert
        assertTrue(result);
    }

    /**
     * Edge case: Case sensitivity test
     */
    @Test
    void isTrustedSource_isCaseSensitive() {
        // Act - Uppercase domain
        boolean result = urlUtil.isTrustedSource("https://www.WTO.ORG/article");

        // Assert - Should be false because contains() is case-sensitive
        assertFalse(result);
    }
}
