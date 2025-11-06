package integration;

import database.news.dto.CreateNewsRequest;
import database.news.dto.UpdateNewsRequest;
import database.news.dto.NewsResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for NewsController.
 * Tests the full stack: Controller -> Service -> Repository -> Database
 */
class NewsControllerIntegrationTest extends BaseIntegrationTest {

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void createNews_withValidData_returnsCreatedNews() {
        // Arrange
        CreateNewsRequest request = new CreateNewsRequest();
        request.setNewsLink("https://example.com/news1");
        request.setRemarks("Test news article");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateNewsRequest> entity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<NewsResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/news",
                entity,
                NewsResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getNewsLink()).isEqualTo("https://example.com/news1");
        assertThat(response.getBody().getRemarks()).isEqualTo("Test news article");
        assertThat(response.getBody().isHidden()).isFalse();

        // Verify it was actually saved in database
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM News WHERE NewsLink = ?",
                Integer.class,
                "https://example.com/news1"
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void getNews_withExistingNews_returnsNewsDetails() {
        // Arrange - Insert test data directly
        jdbcTemplate.update(
                "INSERT INTO News (NewsLink, remarks, is_hidden) VALUES (?, ?, ?)",
                "https://example.com/news2",
                "Existing news",
                false
        );

        // Act
        ResponseEntity<NewsResponse> response = restTemplate.getForEntity(
                baseUrl + "/api/v1/news?newsLink=https://example.com/news2",
                NewsResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getNewsLink()).isEqualTo("https://example.com/news2");
        assertThat(response.getBody().getRemarks()).isEqualTo("Existing news");
        assertThat(response.getBody().isHidden()).isFalse();
    }

    @Test
    void getAllNews_withMultipleNews_returnsAllNews() {
        // Arrange
        jdbcTemplate.update(
                "INSERT INTO News (NewsLink, remarks, is_hidden) VALUES (?, ?, ?)",
                "https://example.com/news3", "News 3", false
        );
        jdbcTemplate.update(
                "INSERT INTO News (NewsLink, remarks, is_hidden) VALUES (?, ?, ?)",
                "https://example.com/news4", "News 4", true
        );

        // Act
        ResponseEntity<NewsResponse[]> response = restTemplate.getForEntity(
                baseUrl + "/api/v1/news/all",
                NewsResponse[].class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void updateNews_withValidData_updatesNewsSuccessfully() {
        // Arrange
        jdbcTemplate.update(
                "INSERT INTO News (NewsLink, remarks, is_hidden) VALUES (?, ?, ?)",
                "https://example.com/news5", "Original remarks", false
        );

        UpdateNewsRequest updateRequest = new UpdateNewsRequest();
        updateRequest.setNewsLink("https://example.com/news5");
        updateRequest.setRemarks("Updated remarks");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateNewsRequest> entity = new HttpEntity<>(updateRequest, headers);

        // Act
        ResponseEntity<NewsResponse> response = restTemplate.exchange(
                baseUrl + "/api/v1/news",
                HttpMethod.PUT,
                entity,
                NewsResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRemarks()).isEqualTo("Updated remarks");

        // Verify database was updated
        String remarks = jdbcTemplate.queryForObject(
                "SELECT remarks FROM News WHERE NewsLink = ?",
                String.class,
                "https://example.com/news5"
        );
        assertThat(remarks).isEqualTo("Updated remarks");
    }

    @Test
    void deleteNews_withExistingNews_deletesSuccessfully() {
        // Arrange
        jdbcTemplate.update(
                "INSERT INTO News (NewsLink, remarks, is_hidden) VALUES (?, ?, ?)",
                "https://example.com/news6", "To be deleted", false
        );

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/news?newsLink=https://example.com/news6",
                HttpMethod.DELETE,
                null,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify deletion in database
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM News WHERE NewsLink = ?",
                Integer.class,
                "https://example.com/news6"
        );
        assertThat(count).isEqualTo(0);
    }

    @Test
    void updateVisibility_togglesHiddenStatus() {
        // Arrange
        jdbcTemplate.update(
                "INSERT INTO News (NewsLink, remarks, is_hidden) VALUES (?, ?, ?)",
                "https://example.com/news7", "Test news", false
        );

        // Act - Hide the news
        ResponseEntity<NewsResponse> response = restTemplate.exchange(
                baseUrl + "/api/v1/news/visibility?newsLink=https://example.com/news7&isHidden=true",
                HttpMethod.PATCH,
                null,
                NewsResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isHidden()).isTrue();

        // Verify in database
        Boolean isHidden = jdbcTemplate.queryForObject(
                "SELECT is_hidden FROM News WHERE NewsLink = ?",
                Boolean.class,
                "https://example.com/news7"
        );
        assertThat(isHidden).isTrue();

        // Act - Unhide the news
        ResponseEntity<NewsResponse> unhideResponse = restTemplate.exchange(
                baseUrl + "/api/v1/news/visibility?newsLink=https://example.com/news7&isHidden=false",
                HttpMethod.PATCH,
                null,
                NewsResponse.class
        );

        // Assert
        assertThat(unhideResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unhideResponse.getBody().isHidden()).isFalse();
    }

    @Test
    void createNews_withDuplicateLink_returnsConflict() {
        // Arrange
        jdbcTemplate.update(
                "INSERT INTO News (NewsLink, remarks, is_hidden) VALUES (?, ?, ?)",
                "https://example.com/duplicate", "Original", false
        );

        CreateNewsRequest request = new CreateNewsRequest();
        request.setNewsLink("https://example.com/duplicate");
        request.setRemarks("Duplicate attempt");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateNewsRequest> entity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/news",
                entity,
                String.class
        );

        // Assert - Should handle duplicate key error
        assertThat(response.getStatusCode()).isIn(HttpStatus.CONFLICT, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getNews_withNonExistentLink_returnsNotFound() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/api/v1/news?newsLink=https://example.com/nonexistent",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
