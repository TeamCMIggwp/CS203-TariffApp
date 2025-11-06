package integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests.
 * Provides common setup and utilities for testing the full Spring Boot application.
 *
 * Features:
 * - Starts full Spring Boot application with random port
 * - Uses H2 in-memory database for testing
 * - Provides TestRestTemplate for HTTP requests
 * - Cleans database before each test
 */
@SpringBootTest(
    classes = web.AppApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    /**
     * Helper method to create authorization header with Bearer token
     */
    protected String createAuthHeader(String token) {
        return "Bearer " + token;
    }

    /**
     * Helper method to clean up test data
     */
    protected void cleanDatabase() {
        // Clean up tables in correct order (respecting foreign keys)
        jdbcTemplate.execute("DELETE FROM UserHiddenSources");
        jdbcTemplate.execute("DELETE FROM NewsTariffRates");
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM TariffRates");
        jdbcTemplate.execute("DELETE FROM News");
        jdbcTemplate.execute("DELETE FROM accounts");
    }
}
