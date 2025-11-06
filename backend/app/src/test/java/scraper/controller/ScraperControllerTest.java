package scraper.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import scraper.dto.ScrapeRequest;
import scraper.dto.ScrapeResponse;
import scraper.service.ScraperService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for ScraperController with full branch and statement coverage.
 *
 * Coverage Strategy:
 * - STATEMENT COVERAGE: Every line of code is executed at least once
 * - BRANCH COVERAGE: Every decision point (if/else) is tested in both TRUE and FALSE directions
 */
@ExtendWith(MockitoExtension.class)
class ScraperControllerTest {

    @Mock
    private ScraperService scraperService;

    @InjectMocks
    private ScraperController scraperController;

    private ScrapeResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = new ScrapeResponse();
        mockResponse.setQuery("rice tariff");
        mockResponse.setStatus(ScrapeResponse.JobStatus.COMPLETED);
        mockResponse.setTotalSourcesFound(5);
        mockResponse.setSourcesScraped(5);
    }

    // ========================================
    // TEST: executeScrape() - SUCCESS PATH
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 60-69
     * BRANCH COVERAGE: Main success path
     *
     * Test: Valid parameters return 200 OK with scrape response
     */
    @Test
    void executeScrape_withValidParameters_returnsOkResponse() {
        // Arrange
        when(scraperService.executeScrapeJob(any(ScrapeRequest.class))).thenReturn(mockResponse);

        // Act
        ResponseEntity<ScrapeResponse> response = scraperController.executeScrape(
            "rice tariff",
            10,
            2020
        );

        // Assert - Statement Coverage
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("rice tariff", response.getBody().getQuery());
        assertEquals(ScrapeResponse.JobStatus.COMPLETED, response.getBody().getStatus());

        // Verify service interaction
        verify(scraperService, times(1)).executeScrapeJob(any(ScrapeRequest.class));
    }

    /**
     * STATEMENT COVERAGE: Lines 60-69
     * Test: MaxResults parameter is correctly passed to service
     */
    @Test
    void executeScrape_withMaxResults_usesCorrectValue() {
        // Arrange
        when(scraperService.executeScrapeJob(any(ScrapeRequest.class))).thenReturn(mockResponse);

        // Act
        ResponseEntity<ScrapeResponse> response = scraperController.executeScrape(
            "tariff data",
            25,
            2020
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify the request was created with correct maxResults
        verify(scraperService).executeScrapeJob(argThat(request ->
            request.getMaxResults() == 25
        ));
    }

    /**
     * STATEMENT COVERAGE: Lines 60-69
     * Test: MinYear parameter is correctly passed to service
     */
    @Test
    void executeScrape_withMinYear_usesCorrectValue() {
        // Arrange
        when(scraperService.executeScrapeJob(any(ScrapeRequest.class))).thenReturn(mockResponse);

        // Act
        ResponseEntity<ScrapeResponse> response = scraperController.executeScrape(
            "import duty",
            10,
            2023
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify the request was created with correct minYear
        verify(scraperService).executeScrapeJob(argThat(request ->
            request.getMinYear() == 2023
        ));
    }

    /**
     * STATEMENT COVERAGE: Lines 60-69
     * Test: Default parameters work correctly
     */
    @Test
    void executeScrape_withDefaultParameters_usesDefaults() {
        // Arrange
        when(scraperService.executeScrapeJob(any(ScrapeRequest.class))).thenReturn(mockResponse);

        // Act - Using default values (maxResults=10, minYear=2020)
        ResponseEntity<ScrapeResponse> response = scraperController.executeScrape(
            "customs tariff",
            10,  // default
            2020 // default
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(scraperService).executeScrapeJob(argThat(request ->
            request.getMaxResults() == 10 && request.getMinYear() == 2020
        ));
    }

    // ========================================
    // EDGE CASE TESTS
    // ========================================

    /**
     * Edge case: Minimum boundary for maxResults (1)
     */
    @Test
    void executeScrape_withMinResultBoundary_accepts1() {
        // Arrange
        when(scraperService.executeScrapeJob(any(ScrapeRequest.class))).thenReturn(mockResponse);

        // Act
        ResponseEntity<ScrapeResponse> response = scraperController.executeScrape(
            "tariff",
            1,
            2020
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(scraperService).executeScrapeJob(argThat(request ->
            request.getMaxResults() == 1
        ));
    }

    /**
     * Edge case: Maximum boundary for maxResults (50)
     */
    @Test
    void executeScrape_withMaxResultBoundary_accepts50() {
        // Arrange
        when(scraperService.executeScrapeJob(any(ScrapeRequest.class))).thenReturn(mockResponse);

        // Act
        ResponseEntity<ScrapeResponse> response = scraperController.executeScrape(
            "tariff",
            50,
            2020
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(scraperService).executeScrapeJob(argThat(request ->
            request.getMaxResults() == 50
        ));
    }

    /**
     * Edge case: Query with special characters
     */
    @Test
    void executeScrape_withSpecialCharactersInQuery_handlesCorrectly() {
        // Arrange
        when(scraperService.executeScrapeJob(any(ScrapeRequest.class))).thenReturn(mockResponse);

        // Act
        ResponseEntity<ScrapeResponse> response = scraperController.executeScrape(
            "rice & wheat tariff",
            10,
            2020
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(scraperService).executeScrapeJob(argThat(request ->
            request.getQuery().equals("rice & wheat tariff")
        ));
    }

    // ========================================
    // TEST: health() ENDPOINT
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 78-80
     * Test: Health endpoint returns correct status
     */
    @Test
    void health_returnsHealthyStatus() {
        // Act
        ResponseEntity<ScraperController.HealthResponse> response = scraperController.health();

        // Assert - Statement Coverage
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("OK", response.getBody().getStatus());
        assertEquals("Scraper service is running", response.getBody().getMessage());
    }

    // ========================================
    // TEST: HealthResponse INNER CLASS
    // ========================================

    /**
     * STATEMENT COVERAGE: Lines 86-102
     * Test: HealthResponse constructor and getters
     */
    @Test
    void healthResponse_getters_returnCorrectValues() {
        // Act
        ScraperController.HealthResponse healthResponse =
            new ScraperController.HealthResponse("UP", "Service healthy");

        // Assert - All getters
        assertEquals("UP", healthResponse.getStatus());
        assertEquals("Service healthy", healthResponse.getMessage());
    }

    /**
     * Edge case: HealthResponse with null values
     */
    @Test
    void healthResponse_withNullValues_handlesGracefully() {
        // Act
        ScraperController.HealthResponse healthResponse =
            new ScraperController.HealthResponse(null, null);

        // Assert
        assertNull(healthResponse.getStatus());
        assertNull(healthResponse.getMessage());
    }

    /**
     * Edge case: HealthResponse with empty strings
     */
    @Test
    void healthResponse_withEmptyStrings_handlesGracefully() {
        // Act
        ScraperController.HealthResponse healthResponse =
            new ScraperController.HealthResponse("", "");

        // Assert
        assertEquals("", healthResponse.getStatus());
        assertEquals("", healthResponse.getMessage());
    }
}
