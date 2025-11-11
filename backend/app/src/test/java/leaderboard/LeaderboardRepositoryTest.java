package leaderboard;

import leaderboard.entity.LeaderboardEntryEntity;
import leaderboard.repository.LeaderboardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private LeaderboardRepository repository;

    @BeforeEach
    void setUp() {
        repository = new LeaderboardRepository();
        // Use reflection to inject mock JdbcTemplate
        try {
            var field = LeaderboardRepository.class.getDeclaredField("jdbcTemplate");
            field.setAccessible(true);
            field.set(repository, jdbcTemplate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject jdbcTemplate", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void getTop10_withEntries_returnsTopTenOrderedByScore() {
        // Arrange
        List<LeaderboardEntryEntity> expectedEntries = Arrays.asList(
                new LeaderboardEntryEntity(1L, "Player1", 1000),
                new LeaderboardEntryEntity(2L, "Player2", 900)
        );
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(expectedEntries);

        // Act
        List<LeaderboardEntryEntity> result = repository.getTop10();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Player1");
        assertThat(result.get(0).getScore()).isEqualTo(1000);
        verify(jdbcTemplate).query(contains("ORDER BY score DESC"), any(RowMapper.class));
        verify(jdbcTemplate).query(contains("LIMIT 10"), any(RowMapper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getTop10_withNoEntries_returnsEmptyList() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());

        // Act
        List<LeaderboardEntryEntity> result = repository.getTop10();

        // Assert
        assertThat(result).isEmpty();
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void findByName_withExistingName_returnsEntity() {
        // Arrange
        LeaderboardEntryEntity expectedEntity = new LeaderboardEntryEntity(5L, "TestPlayer", 750);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("TestPlayer")))
                .thenReturn(Collections.singletonList(expectedEntity));

        // Act
        Optional<LeaderboardEntryEntity> result = repository.findByName("TestPlayer");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("TestPlayer");
        assertThat(result.get().getScore()).isEqualTo(750);
        verify(jdbcTemplate).query(contains("WHERE name = ?"), any(RowMapper.class), eq("TestPlayer"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void findByName_withNonExistentName_returnsEmpty() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("NonExistent")))
                .thenReturn(Collections.emptyList());

        // Act
        Optional<LeaderboardEntryEntity> result = repository.findByName("NonExistent");

        // Assert
        assertThat(result).isEmpty();
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq("NonExistent"));
    }

    @Test
    void create_withValidData_insertsEntry() {
        // Arrange
        when(jdbcTemplate.update(anyString(), eq("NewPlayer"), eq(850))).thenReturn(1);

        // Act
        repository.create("NewPlayer", 850);

        // Assert
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq("NewPlayer"), eq(850));
        assertThat(sqlCaptor.getValue()).contains("INSERT INTO leaderboard");
        assertThat(sqlCaptor.getValue()).contains("(name, score)");
    }

    @Test
    void create_withZeroScore_insertsEntry() {
        // Arrange
        when(jdbcTemplate.update(anyString(), eq("ZeroPlayer"), eq(0))).thenReturn(1);

        // Act
        repository.create("ZeroPlayer", 0);

        // Assert
        verify(jdbcTemplate).update(anyString(), eq("ZeroPlayer"), eq(0));
    }

    @Test
    void create_withNegativeScore_insertsEntry() {
        // Arrange
        when(jdbcTemplate.update(anyString(), eq("NegativePlayer"), eq(-50))).thenReturn(1);

        // Act
        repository.create("NegativePlayer", -50);

        // Assert
        verify(jdbcTemplate).update(anyString(), eq("NegativePlayer"), eq(-50));
    }

    @Test
    void update_withExistingName_updatesScore() {
        // Arrange
        when(jdbcTemplate.update(anyString(), eq(1000), eq("ExistingPlayer"))).thenReturn(1);

        // Act
        int result = repository.update("ExistingPlayer", 1000);

        // Assert
        assertThat(result).isEqualTo(1);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq(1000), eq("ExistingPlayer"));
        assertThat(sqlCaptor.getValue()).contains("UPDATE leaderboard");
        assertThat(sqlCaptor.getValue()).contains("SET score = ?");
        assertThat(sqlCaptor.getValue()).contains("WHERE name = ?");
    }

    @Test
    void update_withNonExistentName_returnsZero() {
        // Arrange
        when(jdbcTemplate.update(anyString(), eq(500), eq("NonExistent"))).thenReturn(0);

        // Act
        int result = repository.update("NonExistent", 500);

        // Assert
        assertThat(result).isEqualTo(0);
        verify(jdbcTemplate).update(anyString(), eq(500), eq("NonExistent"));
    }

    @Test
    void update_withNewScore_updatesCorrectly() {
        // Arrange
        when(jdbcTemplate.update(anyString(), eq(1500), eq("Player1"))).thenReturn(1);

        // Act
        int result = repository.update("Player1", 1500);

        // Assert
        assertThat(result).isEqualTo(1);
        verify(jdbcTemplate).update(anyString(), eq(1500), eq("Player1"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void rowMapper_withValidResultSet_mapsCorrectly() {
        // Arrange
        // Test that the repository correctly maps query results to entities
        LeaderboardEntryEntity expectedEntity = new LeaderboardEntryEntity(10L, "MappedPlayer", 600);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(Collections.singletonList(expectedEntity));

        // Act
        Optional<LeaderboardEntryEntity> result = repository.findByName("MappedPlayer");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(10L);
        assertThat(result.get().getName()).isEqualTo("MappedPlayer");
        assertThat(result.get().getScore()).isEqualTo(600);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getTop10_usesCorrectSqlQuery() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());

        // Act
        repository.getTop10();

        // Assert
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class));
        String sql = sqlCaptor.getValue();
        assertThat(sql).containsIgnoringCase("SELECT");
        assertThat(sql).containsIgnoringCase("FROM leaderboard");
        assertThat(sql).containsIgnoringCase("ORDER BY score DESC");
        assertThat(sql).containsIgnoringCase("LIMIT 10");
    }

    @SuppressWarnings("unchecked")
    @Test
    void findByName_usesCorrectSqlQuery() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(Collections.emptyList());

        // Act
        repository.findByName("TestQuery");

        // Assert
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq("TestQuery"));
        String sql = sqlCaptor.getValue();
        assertThat(sql).containsIgnoringCase("SELECT");
        assertThat(sql).containsIgnoringCase("FROM leaderboard");
        assertThat(sql).containsIgnoringCase("WHERE name = ?");
    }

    @Test
    void create_withLongName_insertsSuccessfully() {
        // Arrange
        String longName = "VeryLongPlayerNameThatExceedsNormalLimits";
        when(jdbcTemplate.update(anyString(), eq(longName), eq(100))).thenReturn(1);

        // Act
        repository.create(longName, 100);

        // Assert
        verify(jdbcTemplate).update(anyString(), eq(longName), eq(100));
    }

    @Test
    void update_withMaxIntegerScore_updatesSuccessfully() {
        // Arrange
        when(jdbcTemplate.update(anyString(), eq(Integer.MAX_VALUE), eq("MaxScorer"))).thenReturn(1);

        // Act
        int result = repository.update("MaxScorer", Integer.MAX_VALUE);

        // Assert
        assertThat(result).isEqualTo(1);
        verify(jdbcTemplate).update(anyString(), eq(Integer.MAX_VALUE), eq("MaxScorer"));
    }
}
