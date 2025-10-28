package database.news.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import database.news.entity.NewsEntity;

@Repository
public class NewsRepository {
    private static final Logger logger = LoggerFactory.getLogger(NewsRepository.class);

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("appJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * Check if news link exists
     */
    public boolean exists(String newsLink) {
        try {
            String sql = "SELECT COUNT(*) FROM `News` WHERE `NewsLink` = ?";
            
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, newsLink);
            
            return count != null && count > 0;
        } catch (DataAccessException e) {
            logger.error("Error checking news existence: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get news by link
     */
    public NewsEntity getNews(String newsLink) {
        try {
            logger.debug("Querying news for: newsLink={}", newsLink);

            String sql = "SELECT `NewsLink`, `remarks`, `isHidden` FROM `News` WHERE `NewsLink` = ?";

            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                NewsEntity news = new NewsEntity();
                news.setNewsLink(rs.getString("NewsLink"));
                news.setRemarks(rs.getString("remarks"));
                news.setHidden(rs.getBoolean("isHidden"));
                return news;
            }, newsLink);

        } catch (EmptyResultDataAccessException e) {
            logger.info("No news found for link: {}", newsLink);
            return null;
        } catch (DataAccessException e) {
            logger.error("Database error while retrieving news: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get all news
     */
    public java.util.List<NewsEntity> getAllNews() {
        try {
            logger.debug("Querying all news");

            String sql = "SELECT `NewsLink`, `remarks`, `isHidden` FROM `News`";

            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                NewsEntity news = new NewsEntity();
                news.setNewsLink(rs.getString("NewsLink"));
                news.setRemarks(rs.getString("remarks"));
                news.setHidden(rs.getBoolean("isHidden"));
                return news;
            });

        } catch (DataAccessException e) {
            logger.error("Database error while retrieving all news: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get all visible news (not hidden)
     */
    public java.util.List<NewsEntity> getAllVisibleNews() {
        try {
            logger.debug("Querying all visible news");

            String sql = "SELECT `NewsLink`, `remarks`, `isHidden` FROM `News` WHERE `isHidden` = FALSE";

            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                NewsEntity news = new NewsEntity();
                news.setNewsLink(rs.getString("NewsLink"));
                news.setRemarks(rs.getString("remarks"));
                news.setHidden(rs.getBoolean("isHidden"));
                return news;
            });

        } catch (DataAccessException e) {
            logger.error("Database error while retrieving visible news: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Create new news - throws exception if already exists
     */
    public void create(String newsLink, String remarks) {
        try {
            logger.info("Creating new news: newsLink={}, remarks={}", newsLink, remarks);

            // Truncate if exceeds limits
            String newsLinkValue = newsLink;
            if (newsLinkValue.length() > 200) {
                newsLinkValue = newsLinkValue.substring(0, 200);
                logger.warn("NewsLink truncated to 200 characters");
            }

            String remarksValue = remarks;
            if (remarksValue != null && remarksValue.length() > 100) {
                remarksValue = remarksValue.substring(0, 100);
                logger.warn("Remarks truncated to 100 characters");
            }

            String sql = "INSERT INTO `News` (`NewsLink`, `remarks`, `isHidden`) VALUES (?, ?, FALSE)";

            int rowsInserted = jdbcTemplate.update(sql, newsLinkValue, remarksValue);

            logger.info("Successfully created news, rows inserted: {}", rowsInserted);

        } catch (DataAccessException e) {
            logger.error("Error creating news: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Hide a news source - marks it as hidden so it won't show up in search results
     */
    public int hideSource(String newsLink) {
        try {
            logger.info("Hiding news source: newsLink={}", newsLink);

            String sql = "UPDATE `News` SET `isHidden` = TRUE WHERE `NewsLink` = ?";

            int rowsUpdated = jdbcTemplate.update(sql, newsLink);

            logger.info("Hidden {} rows", rowsUpdated);
            return rowsUpdated;

        } catch (DataAccessException e) {
            logger.error("Error hiding news source: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Unhide a news source - makes it visible again
     */
    public int unhideSource(String newsLink) {
        try {
            logger.info("Unhiding news source: newsLink={}", newsLink);

            String sql = "UPDATE `News` SET `isHidden` = FALSE WHERE `NewsLink` = ?";

            int rowsUpdated = jdbcTemplate.update(sql, newsLink);

            logger.info("Unhidden {} rows", rowsUpdated);
            return rowsUpdated;

        } catch (DataAccessException e) {
            logger.error("Error unhiding news source: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update existing news remarks - does NOT check if exists (caller's responsibility)
     */
    public int updateRemarks(String newsLink, String remarks) {
        try {
            logger.info("Updating news: newsLink={}, remarks={}", newsLink, remarks);
            
            // Truncate if exceeds limits
            String remarksValue = remarks;
            if (remarksValue != null && remarksValue.length() > 100) {
                remarksValue = remarksValue.substring(0, 100);
                logger.warn("Remarks truncated to 100 characters");
            }
            
            String sql = "UPDATE `News` SET `remarks` = ? WHERE `NewsLink` = ?";
            
            int rowsUpdated = jdbcTemplate.update(sql, remarksValue, newsLink);
            
            logger.info("Updated {} rows", rowsUpdated);
            return rowsUpdated;
            
        } catch (DataAccessException e) {
            logger.error("Error updating news: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Delete news by link
     */
    public int delete(String newsLink) {
        try {
            logger.info("Deleting news: newsLink={}", newsLink);
            
            String sql = "DELETE FROM `News` WHERE `NewsLink` = ?";
            
            int rowsDeleted = jdbcTemplate.update(sql, newsLink);
            
            logger.info("Deleted {} rows", rowsDeleted);
            return rowsDeleted;
            
        } catch (DataAccessException e) {
            logger.error("Error deleting news: {}", e.getMessage(), e);
            throw e;
        }
    }
}