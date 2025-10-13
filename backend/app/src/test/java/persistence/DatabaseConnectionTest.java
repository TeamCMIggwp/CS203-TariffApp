package persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import web.AppApplication;

@SpringBootTest(classes = AppApplication.class)
@TestPropertySource(properties = {
    // Ensure the optional auth datasource doesn't block context startup during tests
    "auth.datasource.url=",
    "auth.datasource.username=",
    "auth.datasource.password="
})
public class DatabaseConnectionTest {
    @Autowired
    @Qualifier("appJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testDatabaseConnection() {
        // Skip this test if no DB URL is configured in the environment; prevents CI failures
        String url = System.getenv("SPRING_DB_URL");
        if (url == null || url.isBlank()) {
            String bootUrl = System.getenv("SPRING_DATASOURCE_URL");
            Assumptions.assumeTrue(bootUrl != null && !bootUrl.isBlank(),
                    "No SPRING_DB_URL or SPRING_DATASOURCE_URL set; skipping DB connection test");
        }

        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        Assertions.assertEquals(1, one);
    }
}
