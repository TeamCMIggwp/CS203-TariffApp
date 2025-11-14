package config;

import javax.sql.*;

import org.springframework.boot.autoconfigure.jdbc.*;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.*;

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
