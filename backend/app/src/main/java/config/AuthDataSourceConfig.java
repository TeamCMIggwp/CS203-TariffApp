package config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AuthDataSourceConfig {

    /**
     * Provide a JdbcTemplate for auth that uses a dedicated DataSource if
     * auth.datasource.* properties are configured; otherwise, fall back to the
     * application's primary DataSource to avoid missing-bean failures.
     */
    @Bean(name = "authJdbcTemplate")
    public JdbcTemplate authJdbcTemplate(Environment env, DataSource primaryDataSource) {
        String url = env.getProperty("auth.datasource.url");
        if (url == null || url.isBlank()) {
            // No dedicated auth datasource configured; use primary
            return new JdbcTemplate(primaryDataSource);
        }
        DataSourceProperties props = new DataSourceProperties();
        props.setUrl(url);
        String username = env.getProperty("auth.datasource.username");
        String password = env.getProperty("auth.datasource.password");
        String driver = env.getProperty("auth.datasource.driver-class-name");
        if (username != null) props.setUsername(username);
        if (password != null) props.setPassword(password);
        if (driver != null && !driver.isBlank()) props.setDriverClassName(driver);
        DataSource ds = props.initializeDataSourceBuilder().build();
        return new JdbcTemplate(ds);
    }
}
