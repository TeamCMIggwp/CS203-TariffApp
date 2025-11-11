package integration;

import leaderboard.dto.LeaderboardRequest;
import leaderboard.dto.LeaderboardResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = web.AppApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LeaderboardControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    @Qualifier("appJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;

        // Clean leaderboard table before each test
        jdbcTemplate.execute("DELETE FROM leaderboard");
    }

    @Test
    void getTop10_withEmptyLeaderboard_returnsEmptyList() {
        // Act
        ResponseEntity<List<LeaderboardResponse>> response = restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<LeaderboardResponse>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void submitScore_withNewPlayer_createsEntry() {
        // Arrange
        LeaderboardRequest request = new LeaderboardRequest("TestPlayer", 500);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LeaderboardRequest> entity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<LeaderboardResponse> response = restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.PUT,
                entity,
                LeaderboardResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("TestPlayer");
        assertThat(response.getBody().getScore()).isEqualTo(500);
        assertThat(response.getBody().getId()).isNotNull();
    }

    @Test
    void submitScore_withExistingPlayer_updatesScore() {
        // Arrange - Create initial entry
        LeaderboardRequest initialRequest = new LeaderboardRequest("UpdatePlayer", 300);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.PUT,
                new HttpEntity<>(initialRequest, headers),
                LeaderboardResponse.class
        );

        // Act - Update with higher score
        LeaderboardRequest updateRequest = new LeaderboardRequest("UpdatePlayer", 800);
        ResponseEntity<LeaderboardResponse> response = restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers),
                LeaderboardResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("UpdatePlayer");
        assertThat(response.getBody().getScore()).isEqualTo(800);

        // Verify only one entry exists
        ResponseEntity<List<LeaderboardResponse>> allEntries = restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<LeaderboardResponse>>() {}
        );
        assertThat(allEntries.getBody()).hasSize(1);
    }

    @Test
    void getTop10_withMultipleEntries_returnsTop10OrderedByScore() {
        // Arrange - Create 15 entries
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (int i = 1; i <= 15; i++) {
            LeaderboardRequest request = new LeaderboardRequest("Player" + i, i * 100);
            restTemplate.exchange(
                    baseUrl + "/leaderboard",
                    HttpMethod.PUT,
                    new HttpEntity<>(request, headers),
                    LeaderboardResponse.class
            );
        }

        // Act
        ResponseEntity<List<LeaderboardResponse>> response = restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<LeaderboardResponse>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(10);

        List<LeaderboardResponse> leaderboard = response.getBody();
        // Verify ordering (highest scores first)
        assertThat(leaderboard.get(0).getScore()).isEqualTo(1500); // Player15
        assertThat(leaderboard.get(1).getScore()).isEqualTo(1400); // Player14
        assertThat(leaderboard.get(9).getScore()).isEqualTo(600);  // Player6

        // Verify scores are in descending order
        for (int i = 0; i < leaderboard.size() - 1; i++) {
            assertThat(leaderboard.get(i).getScore())
                    .isGreaterThanOrEqualTo(leaderboard.get(i + 1).getScore());
        }
    }

    @Test
    void submitScore_withZeroScore_createsEntry() {
        // Arrange
        LeaderboardRequest request = new LeaderboardRequest("ZeroPlayer", 0);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Act
        ResponseEntity<LeaderboardResponse> response = restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.PUT,
                new HttpEntity<>(request, headers),
                LeaderboardResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getScore()).isEqualTo(0);
    }

    @Test
    void submitScore_withNegativeScore_createsEntry() {
        // Arrange
        LeaderboardRequest request = new LeaderboardRequest("NegativePlayer", -50);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Act
        ResponseEntity<LeaderboardResponse> response = restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.PUT,
                new HttpEntity<>(request, headers),
                LeaderboardResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getScore()).isEqualTo(-50);
    }

    @Test
    void submitScore_withVeryHighScore_createsEntry() {
        // Arrange
        LeaderboardRequest request = new LeaderboardRequest("HighScorer", 999999);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Act
        ResponseEntity<LeaderboardResponse> response = restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.PUT,
                new HttpEntity<>(request, headers),
                LeaderboardResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getScore()).isEqualTo(999999);
    }

    @Test
    void getTop10_withFewerThan10Entries_returnsAllEntries() {
        // Arrange - Create only 5 entries
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (int i = 1; i <= 5; i++) {
            LeaderboardRequest request = new LeaderboardRequest("Player" + i, i * 100);
            restTemplate.exchange(
                    baseUrl + "/leaderboard",
                    HttpMethod.PUT,
                    new HttpEntity<>(request, headers),
                    LeaderboardResponse.class
            );
        }

        // Act
        ResponseEntity<List<LeaderboardResponse>> response = restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<LeaderboardResponse>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(5);

        // Verify ordering
        List<LeaderboardResponse> leaderboard = response.getBody();
        assertThat(leaderboard.get(0).getScore()).isEqualTo(500); // Player5
        assertThat(leaderboard.get(4).getScore()).isEqualTo(100); // Player1
    }

    @Test
    void submitScore_multiplePlayers_withSameScore_bothAppearInLeaderboard() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        LeaderboardRequest request1 = new LeaderboardRequest("Player1", 500);
        LeaderboardRequest request2 = new LeaderboardRequest("Player2", 500);

        restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.PUT,
                new HttpEntity<>(request1, headers),
                LeaderboardResponse.class
        );

        restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.PUT,
                new HttpEntity<>(request2, headers),
                LeaderboardResponse.class
        );

        // Act
        ResponseEntity<List<LeaderboardResponse>> response = restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<LeaderboardResponse>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getScore()).isEqualTo(500);
        assertThat(response.getBody().get(1).getScore()).isEqualTo(500);
    }

    @Test
    void submitScore_updatingToLowerScore_updatesSuccessfully() {
        // Arrange - Create entry with high score
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        LeaderboardRequest initialRequest = new LeaderboardRequest("DropPlayer", 900);
        restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.PUT,
                new HttpEntity<>(initialRequest, headers),
                LeaderboardResponse.class
        );

        // Act - Update to lower score
        LeaderboardRequest updateRequest = new LeaderboardRequest("DropPlayer", 200);
        ResponseEntity<LeaderboardResponse> response = restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers),
                LeaderboardResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getScore()).isEqualTo(200);
    }

    @Test
    void submitScore_withLongPlayerName_createsEntry() {
        // Arrange
        String longName = "VeryLongPlayerNameThatExceedsNormalExpectations";
        LeaderboardRequest request = new LeaderboardRequest(longName, 400);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Act
        ResponseEntity<LeaderboardResponse> response = restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.PUT,
                new HttpEntity<>(request, headers),
                LeaderboardResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo(longName);
    }

    @Test
    void getTop10_afterMultipleUpdates_reflectsLatestScores() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create Player1 with score 100
        restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.PUT,
                new HttpEntity<>(new LeaderboardRequest("Player1", 100), headers),
                LeaderboardResponse.class
        );

        // Create Player2 with score 200
        restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.PUT,
                new HttpEntity<>(new LeaderboardRequest("Player2", 200), headers),
                LeaderboardResponse.class
        );

        // Update Player1 to score 300
        restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.PUT,
                new HttpEntity<>(new LeaderboardRequest("Player1", 300), headers),
                LeaderboardResponse.class
        );

        // Act
        ResponseEntity<List<LeaderboardResponse>> response = restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<LeaderboardResponse>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);

        // Player1 should now be first with score 300
        List<LeaderboardResponse> leaderboard = response.getBody();
        assertThat(leaderboard.get(0).getName()).isEqualTo("Player1");
        assertThat(leaderboard.get(0).getScore()).isEqualTo(300);
        assertThat(leaderboard.get(1).getName()).isEqualTo("Player2");
        assertThat(leaderboard.get(1).getScore()).isEqualTo(200);
    }

    @Test
    void submitScore_withSpecialCharactersInName_createsEntry() {
        // Arrange
        String specialName = "Player@#$%^&*()";
        LeaderboardRequest request = new LeaderboardRequest(specialName, 350);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Act
        ResponseEntity<LeaderboardResponse> response = restTemplate.exchange(
                baseUrl + "/leaderboard",
                HttpMethod.PUT,
                new HttpEntity<>(request, headers),
                LeaderboardResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo(specialName);
    }
}
