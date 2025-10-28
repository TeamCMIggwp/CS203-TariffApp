// package persistence;

// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.jdbc.core.JdbcTemplate;

// import web.AppApplication;

// @EnabledIfEnvironmentVariable(named = "SPRING_DB_URL", matches = ".+")
// @SpringBootTest(classes = AppApplication.class)
// public class DatabaseConnectionTest {
//     @Autowired
//     @Qualifier("appJdbcTemplate")
//     private JdbcTemplate jdbcTemplate;

//     @Test
//     public void testDatabaseConnection() {
//         jdbcTemplate.query(
//                 "SELECT * FROM Products LIMIT 5",
//                 (rs, rowNum) -> String.format(
//                         "Product ID: %d, HS Code: %s, Name: %s",
//                         rs.getInt("codeUnique"),
//                         rs.getString("hs_code"),
//                         rs.getString("name")))
//                 .forEach(System.out::println);
//     }
// }
