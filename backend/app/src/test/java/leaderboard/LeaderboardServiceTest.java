package leaderboard;

import leaderboard.dto.LeaderboardRequest;
import leaderboard.dto.LeaderboardResponse;
import leaderboard.entity.LeaderboardEntryEntity;
import leaderboard.repository.LeaderboardRepository;
import leaderboard.service.LeaderboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private LeaderboardRepository repository;

    private LeaderboardService service;

    @BeforeEach
    void setUp() {
        service = new LeaderboardService(repository);
    }

    @Test
    void getTop10_withMultipleEntries_returnsTop10() {
        // Arrange
        List<LeaderboardEntryEntity> entries = Arrays.asList(
                new LeaderboardEntryEntity(1L, "Player1", 1000),
                new LeaderboardEntryEntity(2L, "Player2", 950),
                new LeaderboardEntryEntity(3L, "Player3", 900),
                new LeaderboardEntryEntity(4L, "Player4", 850),
                new LeaderboardEntryEntity(5L, "Player5", 800),
                new LeaderboardEntryEntity(6L, "Player6", 750),
                new LeaderboardEntryEntity(7L, "Player7", 700),
                new LeaderboardEntryEntity(8L, "Player8", 650),
                new LeaderboardEntryEntity(9L, "Player9", 600),
                new LeaderboardEntryEntity(10L, "Player10", 550)
        );
        when(repository.getTop10()).thenReturn(entries);

        // Act
        List<LeaderboardResponse> result = service.getTop10();

        // Assert
        assertThat(result).hasSize(10);
        assertThat(result.get(0).getName()).isEqualTo("Player1");
        assertThat(result.get(0).getScore()).isEqualTo(1000);
        assertThat(result.get(9).getName()).isEqualTo("Player10");
        assertThat(result.get(9).getScore()).isEqualTo(550);
        verify(repository).getTop10();
    }

    @Test
    void getTop10_withNoEntries_returnsEmptyList() {
        // Arrange
        when(repository.getTop10()).thenReturn(Collections.emptyList());

        // Act
        List<LeaderboardResponse> result = service.getTop10();

        // Assert
        assertThat(result).isEmpty();
        verify(repository).getTop10();
    }

    @Test
    void getTop10_withFewerThan10Entries_returnsAllEntries() {
        // Arrange
        List<LeaderboardEntryEntity> entries = Arrays.asList(
                new LeaderboardEntryEntity(1L, "Player1", 1000),
                new LeaderboardEntryEntity(2L, "Player2", 950),
                new LeaderboardEntryEntity(3L, "Player3", 900)
        );
        when(repository.getTop10()).thenReturn(entries);

        // Act
        List<LeaderboardResponse> result = service.getTop10();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("Player1");
        assertThat(result.get(2).getName()).isEqualTo("Player3");
        verify(repository).getTop10();
    }

    @Test
    void submitScore_withNewPlayer_createsNewEntry() {
        // Arrange
        LeaderboardRequest request = new LeaderboardRequest("NewPlayer", 800);
        LeaderboardEntryEntity savedEntity = new LeaderboardEntryEntity(1L, "NewPlayer", 800);

        when(repository.update(anyString(), anyInt())).thenReturn(0);
        when(repository.create(anyString(), anyInt())).thenReturn(1);
        when(repository.findByName("NewPlayer")).thenReturn(Optional.of(savedEntity));

        // Act
        LeaderboardResponse result = service.submitScore(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("NewPlayer");
        assertThat(result.getScore()).isEqualTo(800);
        verify(repository).update("NewPlayer", 800);
        verify(repository).create("NewPlayer", 800);
        verify(repository).findByName("NewPlayer");
    }

    @Test
    void submitScore_withExistingPlayer_updatesScore() {
        // Arrange
        LeaderboardRequest request = new LeaderboardRequest("ExistingPlayer", 1200);
        LeaderboardEntryEntity updatedEntity = new LeaderboardEntryEntity(5L, "ExistingPlayer", 1200);

        when(repository.update("ExistingPlayer", 1200)).thenReturn(1);
        when(repository.findByName("ExistingPlayer")).thenReturn(Optional.of(updatedEntity));

        // Act
        LeaderboardResponse result = service.submitScore(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getName()).isEqualTo("ExistingPlayer");
        assertThat(result.getScore()).isEqualTo(1200);
        verify(repository).update("ExistingPlayer", 1200);
        verify(repository, never()).create(anyString(), anyInt());
        verify(repository).findByName("ExistingPlayer");
    }

    @Test
    void submitScore_whenFindByNameReturnsEmpty_throwsRuntimeException() {
        // Arrange
        LeaderboardRequest request = new LeaderboardRequest("FailPlayer", 500);

        when(repository.update(anyString(), anyInt())).thenReturn(0);
        when(repository.create(anyString(), anyInt())).thenReturn(1);
        when(repository.findByName("FailPlayer")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.submitScore(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to save leaderboard entry");

        verify(repository).update("FailPlayer", 500);
        verify(repository).create("FailPlayer", 500);
        verify(repository).findByName("FailPlayer");
    }

    @Test
    void submitScore_withZeroScore_createsEntry() {
        // Arrange
        LeaderboardRequest request = new LeaderboardRequest("ZeroPlayer", 0);
        LeaderboardEntryEntity savedEntity = new LeaderboardEntryEntity(2L, "ZeroPlayer", 0);

        when(repository.update(anyString(), anyInt())).thenReturn(0);
        when(repository.create(anyString(), anyInt())).thenReturn(1);
        when(repository.findByName("ZeroPlayer")).thenReturn(Optional.of(savedEntity));

        // Act
        LeaderboardResponse result = service.submitScore(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getScore()).isEqualTo(0);
        verify(repository).create("ZeroPlayer", 0);
    }

    @Test
    void submitScore_withNegativeScore_createsEntry() {
        // Arrange
        LeaderboardRequest request = new LeaderboardRequest("NegativePlayer", -100);
        LeaderboardEntryEntity savedEntity = new LeaderboardEntryEntity(3L, "NegativePlayer", -100);

        when(repository.update(anyString(), anyInt())).thenReturn(0);
        when(repository.create(anyString(), anyInt())).thenReturn(1);
        when(repository.findByName("NegativePlayer")).thenReturn(Optional.of(savedEntity));

        // Act
        LeaderboardResponse result = service.submitScore(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getScore()).isEqualTo(-100);
        verify(repository).create("NegativePlayer", -100);
    }

    @Test
    void submitScore_withVeryHighScore_createsEntry() {
        // Arrange
        LeaderboardRequest request = new LeaderboardRequest("HighScorer", 999999);
        LeaderboardEntryEntity savedEntity = new LeaderboardEntryEntity(4L, "HighScorer", 999999);

        when(repository.update(anyString(), anyInt())).thenReturn(0);
        when(repository.create(anyString(), anyInt())).thenReturn(1);
        when(repository.findByName("HighScorer")).thenReturn(Optional.of(savedEntity));

        // Act
        LeaderboardResponse result = service.submitScore(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getScore()).isEqualTo(999999);
    }

    @Test
    void submitScore_updateReturnsMultipleRows_doesNotCreateNew() {
        // Arrange (edge case: update affects multiple rows)
        LeaderboardRequest request = new LeaderboardRequest("DuplicatePlayer", 700);
        LeaderboardEntryEntity savedEntity = new LeaderboardEntryEntity(6L, "DuplicatePlayer", 700);

        when(repository.update("DuplicatePlayer", 700)).thenReturn(2);
        when(repository.findByName("DuplicatePlayer")).thenReturn(Optional.of(savedEntity));

        // Act
        LeaderboardResponse result = service.submitScore(request);

        // Assert
        assertThat(result).isNotNull();
        verify(repository).update("DuplicatePlayer", 700);
        verify(repository, never()).create(anyString(), anyInt());
        verify(repository).findByName("DuplicatePlayer");
    }

    @Test
    void getTop10_mapsEntityFieldsCorrectly() {
        // Arrange
        LeaderboardEntryEntity entity = new LeaderboardEntryEntity(100L, "TestPlayer", 1500);
        when(repository.getTop10()).thenReturn(Collections.singletonList(entity));

        // Act
        List<LeaderboardResponse> result = service.getTop10();

        // Assert
        assertThat(result).hasSize(1);
        LeaderboardResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getName()).isEqualTo("TestPlayer");
        assertThat(response.getScore()).isEqualTo(1500);
    }
}
