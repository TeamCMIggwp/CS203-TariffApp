package database.userhiddensources.service;

import database.userhiddensources.dto.HiddenSourceResponse;
import database.userhiddensources.entity.UserHiddenSourcesEntity;
import database.userhiddensources.repository.UserHiddenSourcesRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserHiddenSourcesServiceTest {

    @Mock
    private UserHiddenSourcesRepository repository;

    @InjectMocks
    private UserHiddenSourcesService service;

    private static final String USER_ID = "user123";
    private static final String NEWS_LINK = "https://example.com/news/1";

    @Test
    void hideSourceForUser_success() {
        // When
        HiddenSourceResponse response = service.hideSourceForUser(USER_ID, NEWS_LINK);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getNewsLink()).isEqualTo(NEWS_LINK);
        assertThat(response.getHiddenAt()).isNotNull();

        verify(repository).hideSource(USER_ID, NEWS_LINK);
    }

    @Test
    void unhideSourceForUser_success() {
        // Given
        when(repository.unhideSource(USER_ID, NEWS_LINK)).thenReturn(1);

        // When
        service.unhideSourceForUser(USER_ID, NEWS_LINK);

        // Then
        verify(repository).unhideSource(USER_ID, NEWS_LINK);
    }

    @Test
    void unhideSourceForUser_notHidden_logsWarning() {
        // Given
        when(repository.unhideSource(USER_ID, NEWS_LINK)).thenReturn(0);

        // When
        service.unhideSourceForUser(USER_ID, NEWS_LINK);

        // Then
        verify(repository).unhideSource(USER_ID, NEWS_LINK);
    }

    @Test
    void unhideAllSourcesForUser_success() {
        // Given
        when(repository.unhideAllSources(USER_ID)).thenReturn(3);

        // When
        int count = service.unhideAllSourcesForUser(USER_ID);

        // Then
        assertThat(count).isEqualTo(3);
        verify(repository).unhideAllSources(USER_ID);
    }

    @Test
    void getHiddenSourcesForUser_returnsHiddenSources() {
        // Given
        UserHiddenSourcesEntity entity1 = new UserHiddenSourcesEntity();
        entity1.setUserId(USER_ID);
        entity1.setNewsLink("https://example.com/news/1");
        entity1.setHiddenAt(LocalDateTime.now());

        UserHiddenSourcesEntity entity2 = new UserHiddenSourcesEntity();
        entity2.setUserId(USER_ID);
        entity2.setNewsLink("https://example.com/news/2");
        entity2.setHiddenAt(LocalDateTime.now());

        when(repository.getAllHiddenSourcesByUser(USER_ID)).thenReturn(Arrays.asList(entity1, entity2));

        // When
        List<HiddenSourceResponse> responses = service.getHiddenSourcesForUser(USER_ID);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getNewsLink()).isEqualTo("https://example.com/news/1");
        assertThat(responses.get(1).getNewsLink()).isEqualTo("https://example.com/news/2");

        verify(repository).getAllHiddenSourcesByUser(USER_ID);
    }

    @Test
    void isSourceHiddenByUser_returnsTrue() {
        // Given
        when(repository.isHiddenByUser(USER_ID, NEWS_LINK)).thenReturn(true);

        // When
        boolean isHidden = service.isSourceHiddenByUser(USER_ID, NEWS_LINK);

        // Then
        assertThat(isHidden).isTrue();
        verify(repository).isHiddenByUser(USER_ID, NEWS_LINK);
    }

    @Test
    void isSourceHiddenByUser_returnsFalse() {
        // Given
        when(repository.isHiddenByUser(USER_ID, NEWS_LINK)).thenReturn(false);

        // When
        boolean isHidden = service.isSourceHiddenByUser(USER_ID, NEWS_LINK);

        // Then
        assertThat(isHidden).isFalse();
        verify(repository).isHiddenByUser(USER_ID, NEWS_LINK);
    }
}
