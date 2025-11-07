package database.news.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Map;

/**
 * One-time migration utility to normalize existing URLs in the News table.
 * This runs automatically on application startup.
 *
 * To disable, set: app.migration.normalize-urls=false in application.properties
 */
@Component
public class NewsUrlMigration {

    private static final Logger logger = LoggerFactory.getLogger(NewsUrlMigration.class);

    @Autowired
    @Qualifier("appJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @org.springframework.beans.factory.annotation.Value("${app.migration.normalize-urls:true}")
    private boolean migrationEnabled;

    @PostConstruct
    public void migrateUrls() {
        if (!migrationEnabled) {
            logger.info("URL normalization migration is disabled");
            return;
        }

        try {
            logger.info("Starting URL normalization migration for News table...");

            // Get all news links
            String selectSql = "SELECT `NewsLink` FROM `News`";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(selectSql);

            int totalCount = results.size();
            int updatedCount = 0;
            int errorCount = 0;

            logger.info("Found {} URLs to check", totalCount);

            for (Map<String, Object> row : results) {
                String originalUrl = (String) row.get("NewsLink");

                if (originalUrl == null || originalUrl.trim().isEmpty()) {
                    continue;
                }

                String normalizedUrl = UrlNormalizer.normalize(originalUrl);

                // Only update if the URL changed after normalization
                if (!originalUrl.equals(normalizedUrl)) {
                    try {
                        // Check if normalized URL already exists (to avoid duplicate key errors)
                        String checkSql = "SELECT COUNT(*) FROM `News` WHERE `NewsLink` = ?";
                        Integer existingCount = jdbcTemplate.queryForObject(checkSql, Integer.class, normalizedUrl);

                        if (existingCount != null && existingCount > 0) {
                            // Normalized URL already exists, need to handle duplicate
                            logger.warn("Skipping normalization of '{}' -> '{}' (target already exists)",
                                    originalUrl, normalizedUrl);
                            continue;
                        }

                        // Update the URL
                        String updateSql = "UPDATE `News` SET `NewsLink` = ? WHERE `NewsLink` = ?";
                        int rowsUpdated = jdbcTemplate.update(updateSql, normalizedUrl, originalUrl);

                        if (rowsUpdated > 0) {
                            updatedCount++;
                            logger.info("Normalized URL: '{}' -> '{}'", originalUrl, normalizedUrl);
                        }

                    } catch (Exception e) {
                        errorCount++;
                        logger.error("Error normalizing URL '{}': {}", originalUrl, e.getMessage());
                    }
                }
            }

            logger.info("URL normalization migration completed. Total: {}, Updated: {}, Errors: {}",
                    totalCount, updatedCount, errorCount);

        } catch (Exception e) {
            logger.error("URL normalization migration failed: {}", e.getMessage(), e);
        }
    }
}
