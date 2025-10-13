package config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AuthDataSourceConfig {

    @Bean
    @ConfigurationProperties("auth.datasource")
    public DataSourceProperties authDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "authDataSource")
    public DataSource authDataSource(DataSourceProperties authDataSourceProperties) {
        return authDataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "authJdbcTemplate")
    public JdbcTemplate authJdbcTemplate(DataSource authDataSource) {
        return new JdbcTemplate(authDataSource);
    }
}
