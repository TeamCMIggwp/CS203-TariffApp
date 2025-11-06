package integration;

import database.userhiddensources.dto.HiddenSourceResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UserHiddenSourcesController.
 * Tests user-specific hidden sources management with authentication
 */
class UserHiddenSourcesControllerIntegrationTest extends BaseIntegrationTest {

    @BeforeEach
    void setUpUserHiddenSources() {
        // Ensure tables exist
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS News (" +
                "  NewsLink VARCHAR(512) PRIMARY KEY," +
                "  remarks VARCHAR(100)," +
                "  is_hidden BOOLEAN DEFAULT FALSE" +
                ")"
            );

            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS UserHiddenSources (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id VARCHAR(255) NOT NULL," +
                "  news_link VARCHAR(512) NOT NULL," +
                "  hidden_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  UNIQUE(user_id, news_link)" +
                ")"
            );
        } catch (Exception e) {
            // Tables might already exist
        }

        // Insert test news articles
        jdbcTemplate.update(
            "INSERT INTO News (NewsLink, remarks, is_hidden) VALUES (?, ?, ?)",
            "https://example.com/news1", "Test News 1", false
        );
        jdbcTemplate.update(
            "INSERT INTO News (NewsLink, remarks, is_hidden) VALUES (?, ?, ?)",
            "https://example.com/news2", "Test News 2", false
        );
        jdbcTemplate.update(
            "INSERT INTO News (NewsLink, remarks, is_hidden) VALUES (?, ?, ?)",
            "https://example.com/news3", "Test News 3", false
        );
    }

    @AfterEach
    void tearDownUserHiddenSources() {
        try {
            jdbcTemplate.execute("DELETE FROM UserHiddenSources");
            jdbcTemplate.execute("DELETE FROM News");
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        cleanDatabase();
    }

    @Test
    @WithMockUser(username = "user123", roles = {"USER"})
    void hideSource_withAuthenticatedUser_hidesSourceSuccessfully() {
        // Arrange
        String newsLink = "https://example.com/news1";

        // Act
        ResponseEntity<HiddenSourceResponse> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/user/news?newsLink=" + newsLink,
            null,
            HiddenSourceResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getNewsLink()).isEqualTo(newsLink);

        // Verify in database
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM UserHiddenSources WHERE user_id = ? AND news_link = ?",
            Integer.class,
            "user123", newsLink
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "user456", roles = {"USER"})
    void getHiddenSources_withMultipleSources_returnsAllUserSources() {
        // Arrange - Hide multiple sources for the user
        jdbcTemplate.update(
            "INSERT INTO UserHiddenSources (user_id, news_link) VALUES (?, ?)",
            "user456", "https://example.com/news1"
        );
        jdbcTemplate.update(
            "INSERT INTO UserHiddenSources (user_id, news_link) VALUES (?, ?)",
            "user456", "https://example.com/news2"
        );
        // Add a source for a different user (should not be returned)
        jdbcTemplate.update(
            "INSERT INTO UserHiddenSources (user_id, news_link) VALUES (?, ?)",
            "differentUser", "https://example.com/news3"
        );

        // Act
        ResponseEntity<HiddenSourceResponse[]> response = restTemplate.getForEntity(
            baseUrl + "/api/v1/user/news",
            HiddenSourceResponse[].class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody())
            .extracting(HiddenSourceResponse::getNewsLink)
            .containsExactlyInAnyOrder(
                "https://example.com/news1",
                "https://example.com/news2"
            );
    }

    @Test
    @WithMockUser(username = "user789", roles = {"USER"})
    void unhideSource_withSpecificLink_removesHiddenSource() {
        // Arrange
        String newsLink = "https://example.com/news1";
        jdbcTemplate.update(
            "INSERT INTO UserHiddenSources (user_id, news_link) VALUES (?, ?)",
            "user789", newsLink
        );

        // Verify it was inserted
        Integer countBefore = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM UserHiddenSources WHERE user_id = ? AND news_link = ?",
            Integer.class,
            "user789", newsLink
        );
        assertThat(countBefore).isEqualTo(1);

        // Act
        ResponseEntity<Void> response = restTemplate.exchange(
            baseUrl + "/api/v1/user/news?newsLink=" + newsLink,
            HttpMethod.DELETE,
            null,
            Void.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify deletion
        Integer countAfter = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM UserHiddenSources WHERE user_id = ? AND news_link = ?",
            Integer.class,
            "user789", newsLink
        );
        assertThat(countAfter).isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "user999", roles = {"USER"})
    void unhideAllSources_removesAllUserSources() {
        // Arrange - Hide multiple sources for the user
        jdbcTemplate.update(
            "INSERT INTO UserHiddenSources (user_id, news_link) VALUES (?, ?)",
            "user999", "https://example.com/news1"
        );
        jdbcTemplate.update(
            "INSERT INTO UserHiddenSources (user_id, news_link) VALUES (?, ?)",
            "user999", "https://example.com/news2"
        );
        jdbcTemplate.update(
            "INSERT INTO UserHiddenSources (user_id, news_link) VALUES (?, ?)",
            "user999", "https://example.com/news3"
        );
        // Add a source for a different user (should NOT be deleted)
        jdbcTemplate.update(
            "INSERT INTO UserHiddenSources (user_id, news_link) VALUES (?, ?)",
            "otherUser", "https://example.com/news1"
        );

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/api/v1/user/news?all=true",
            HttpMethod.DELETE,
            null,
            String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Unhidden 3 sources");

        // Verify all user999 sources were deleted
        Integer countUser999 = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM UserHiddenSources WHERE user_id = ?",
            Integer.class,
            "user999"
        );
        assertThat(countUser999).isEqualTo(0);

        // Verify other user's source is still there
        Integer countOtherUser = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM UserHiddenSources WHERE user_id = ?",
            Integer.class,
            "otherUser"
        );
        assertThat(countOtherUser).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "userABC", roles = {"USER"})
    void hideSource_duplicateHide_handlesGracefully() {
        // Arrange
        String newsLink = "https://example.com/news1";

        // Hide once
        restTemplate.postForEntity(
            baseUrl + "/api/v1/user/news?newsLink=" + newsLink,
            null,
            HiddenSourceResponse.class
        );

        // Act - Try to hide again
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/user/news?newsLink=" + newsLink,
            null,
            String.class
        );

        // Assert - Should either succeed or return conflict
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.CONFLICT);

        // Verify only one entry exists
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM UserHiddenSources WHERE user_id = ? AND news_link = ?",
            Integer.class,
            "userABC", newsLink
        );
        assertThat(count).isLessThanOrEqualTo(1);
    }

    @Test
    @WithMockUser(username = "userDEF", roles = {"USER"})
    void getHiddenSources_withNoHiddenSources_returnsEmptyList() {
        // Act
        ResponseEntity<HiddenSourceResponse[]> response = restTemplate.getForEntity(
            baseUrl + "/api/v1/user/news",
            HiddenSourceResponse[].class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @WithMockUser(username = "userGHI", roles = {"USER"})
    void unhideSource_withMissingNewsLink_returnsBadRequest() {
        // Act - Try to unhide without providing newsLink parameter
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/api/v1/user/news?all=false",
            HttpMethod.DELETE,
            null,
            String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("newsLink parameter required");
    }

    @Test
    @WithMockUser(username = "userJKL", roles = {"USER"})
    void hideSource_withMissingNewsLink_returnsBadRequest() {
        // Act - Try to hide without providing newsLink parameter
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/user/news",
            null,
            String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @WithMockUser(username = "userMNO", roles = {"USER"})
    void userIsolation_differentUsersCannotSeeEachOthersHiddenSources() {
        // Arrange - User1 hides a source
        jdbcTemplate.update(
            "INSERT INTO UserHiddenSources (user_id, news_link) VALUES (?, ?)",
            "user1", "https://example.com/news1"
        );
        // User2 hides a different source
        jdbcTemplate.update(
            "INSERT INTO UserHiddenSources (user_id, news_link) VALUES (?, ?)",
            "user2", "https://example.com/news2"
        );
        // Current authenticated user (userMNO) has their own hidden source
        jdbcTemplate.update(
            "INSERT INTO UserHiddenSources (user_id, news_link) VALUES (?, ?)",
            "userMNO", "https://example.com/news3"
        );

        // Act - Get hidden sources for current user
        ResponseEntity<HiddenSourceResponse[]> response = restTemplate.getForEntity(
            baseUrl + "/api/v1/user/news",
            HiddenSourceResponse[].class
        );

        // Assert - Should only see their own hidden source
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].getNewsLink()).isEqualTo("https://example.com/news3");

        // Verify in database that it belongs to the correct user
        String actualUserId = jdbcTemplate.queryForObject(
            "SELECT user_id FROM UserHiddenSources WHERE news_link = ?",
            String.class,
            "https://example.com/news3"
        );
        assertThat(actualUserId).isEqualTo("userMNO");
    }
}
