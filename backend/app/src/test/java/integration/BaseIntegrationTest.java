package integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
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
@Import(TestSecurityConfig.class)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    @Qualifier("appJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

    protected String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        cleanDatabase();
        setupTestData();
    }
    
    /**
     * Override this method in test classes to set up any necessary test data
     */
    protected void setupTestData() {
        // Default implementation is empty
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
        try {
            // Disable foreign key checks temporarily
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
            
            // Clean up tables in correct order
            jdbcTemplate.execute("TRUNCATE TABLE UserHiddenSources");
            jdbcTemplate.execute("TRUNCATE TABLE NewsTariffRates");
            jdbcTemplate.execute("TRUNCATE TABLE refresh_tokens");
            jdbcTemplate.execute("TRUNCATE TABLE wto_tariffs.TariffRates");
            jdbcTemplate.execute("TRUNCATE TABLE News");
            jdbcTemplate.execute("TRUNCATE TABLE accounts");
            
            // Re-enable foreign key checks
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        } catch (Exception e) {
            System.err.println("Error cleaning database: " + e.getMessage());
        }
    }
}
