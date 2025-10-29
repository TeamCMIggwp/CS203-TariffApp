package database.userhiddensources.repository;

import database.userhiddensources.entity.UserHiddenSourcesEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class UserHiddenSourcesRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserHiddenSourcesRepository.class);

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("appJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * Check if user has hidden a specific source
     */
    public boolean isHiddenByUser(String userId, String newsLink) {
        try {
            String sql = "SELECT COUNT(*) FROM UserHiddenSources WHERE user_id = ? AND news_link = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, newsLink);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            logger.error("Error checking if source is hidden by user: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get all hidden sources for a user
     */
    public List<UserHiddenSourcesEntity> getAllHiddenSourcesByUser(String userId) {
        try {
            logger.debug("Querying hidden sources for user: {}", userId);

            String sql = "SELECT id, user_id, news_link, hidden_at FROM UserHiddenSources WHERE user_id = ? ORDER BY hidden_at DESC";

            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                UserHiddenSourcesEntity entity = new UserHiddenSourcesEntity();
                entity.setId(rs.getInt("id"));
                entity.setUserId(rs.getString("user_id"));
                entity.setNewsLink(rs.getString("news_link"));
                Timestamp timestamp = rs.getTimestamp("hidden_at");
                if (timestamp != null) {
                    entity.setHiddenAt(timestamp.toLocalDateTime());
                }
                return entity;
            }, userId);

        } catch (DataAccessException e) {
            logger.error("Database error while retrieving hidden sources for user: {}", userId, e);
            throw e;
        }
    }

    /**
     * Hide a source for a user
     */
    public void hideSource(String userId, String newsLink) {
        try {
            logger.info("Hiding source for user: userId={}, newsLink={}", userId, newsLink);

            String sql = "INSERT INTO UserHiddenSources (user_id, news_link) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE hidden_at = CURRENT_TIMESTAMP";

            int rowsInserted = jdbcTemplate.update(sql, userId, newsLink);

            logger.info("Successfully hidden source, rows affected: {}", rowsInserted);

        } catch (DataAccessException e) {
            logger.error("Error hiding source: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Unhide a source for a user
     */
    public int unhideSource(String userId, String newsLink) {
        try {
            logger.info("Unhiding source for user: userId={}, newsLink={}", userId, newsLink);

            String sql = "DELETE FROM UserHiddenSources WHERE user_id = ? AND news_link = ?";

            int rowsDeleted = jdbcTemplate.update(sql, userId, newsLink);

            logger.info("Deleted {} rows", rowsDeleted);
            return rowsDeleted;

        } catch (DataAccessException e) {
            logger.error("Error unhiding source: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Unhide all sources for a user
     */
    public int unhideAllSources(String userId) {
        try {
            logger.info("Unhiding all sources for user: {}", userId);

            String sql = "DELETE FROM UserHiddenSources WHERE user_id = ?";

            int rowsDeleted = jdbcTemplate.update(sql, userId);

            logger.info("Deleted {} rows", rowsDeleted);
            return rowsDeleted;

        } catch (DataAccessException e) {
            logger.error("Error unhiding all sources: {}", e.getMessage(), e);
            throw e;
        }
    }
}
