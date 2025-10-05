package config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Exposes a JdbcTemplate bound to the primary application DataSource (spring.datasource),
 * ensuring repositories can target the main database (wto_tariffs) explicitly when multiple
 * datasources/JdbcTemplates exist (e.g., auth datasource).
 */
@Configuration
public class AppJdbcTemplateConfig {

    // Primary application DataSource (wto_tariffs) from spring.datasource.*
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties appDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "dataSource")
    @Primary
    public DataSource appDataSource(DataSourceProperties appDataSourceProperties) {
        return appDataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "appJdbcTemplate")
    public JdbcTemplate appJdbcTemplate(@org.springframework.beans.factory.annotation.Qualifier("dataSource") DataSource dataSource) {
        // The auto-configured primary DataSource from spring.datasource.*
        return new JdbcTemplate(dataSource);
    }
}
