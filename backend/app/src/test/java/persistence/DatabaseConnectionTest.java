package persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import web.AppApplication;

@SpringBootTest(classes = AppApplication.class)
public class DatabaseConnectionTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testDatabaseConnection() {
        jdbcTemplate.query(
            "SELECT * FROM Products LIMIT 5",
            (rs, rowNum) -> String.format(
                "Product ID: %d, HS Code: %s, Name: %s",
                rs.getInt("product_id"),
                rs.getString("hs_code"),
                rs.getString("name")
            )
        ).forEach(System.out::println);
    }
}
