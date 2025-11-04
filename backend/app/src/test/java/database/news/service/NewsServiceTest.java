package database.news.service;

import database.news.dto.CreateNewsRequest;
import database.news.dto.NewsResponse;
import database.news.dto.UpdateNewsRequest;
import database.news.entity.NewsEntity;
import database.news.exception.NewsAlreadyExistsException;
import database.news.exception.NewsNotFoundException;
import database.news.repository.NewsRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private NewsRepository repository;

    @InjectMocks
    private NewsService service;

    private static final String NEWS_LINK = "https://example.com/news/1";
    private static final String REMARKS = "Test remarks";

    @Test
    void createNews_success() {
        // Given
        CreateNewsRequest request = new CreateNewsRequest();
        request.setNewsLink(NEWS_LINK);
        request.setRemarks(REMARKS);

        when(repository.exists(NEWS_LINK)).thenReturn(false);

        // When
        NewsResponse response = service.createNews(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getNewsLink()).isEqualTo(NEWS_LINK);
        assertThat(response.getRemarks()).isEqualTo(REMARKS);
        assertThat(response.isHidden()).isFalse();

        verify(repository).exists(NEWS_LINK);
        verify(repository).create(NEWS_LINK, REMARKS);
    }

    @Test
    void createNews_alreadyExists_throwsException() {
        // Given
        CreateNewsRequest request = new CreateNewsRequest();
        request.setNewsLink(NEWS_LINK);
        request.setRemarks(REMARKS);

        when(repository.exists(NEWS_LINK)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> service.createNews(request))
                .isInstanceOf(NewsAlreadyExistsException.class);

        verify(repository).exists(NEWS_LINK);
        verify(repository, never()).create(any(), any());
    }

    @Test
    void getNews_found_returnsResponse() {
        // Given
        NewsEntity entity = new NewsEntity();
        entity.setNewsLink(NEWS_LINK);
        entity.setRemarks(REMARKS);
        entity.setHidden(false);

        when(repository.getNews(NEWS_LINK)).thenReturn(entity);

        // When
        NewsResponse response = service.getNews(NEWS_LINK);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getNewsLink()).isEqualTo(NEWS_LINK);
        assertThat(response.getRemarks()).isEqualTo(REMARKS);
        assertThat(response.isHidden()).isFalse();

        verify(repository).getNews(NEWS_LINK);
    }

    @Test
    void getNews_notFound_throwsException() {
        // Given
        when(repository.getNews(NEWS_LINK)).thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> service.getNews(NEWS_LINK))
                .isInstanceOf(NewsNotFoundException.class);

        verify(repository).getNews(NEWS_LINK);
    }

    @Test
    void updateNews_success() {
        // Given
        UpdateNewsRequest request = new UpdateNewsRequest();
        request.setRemarks("Updated remarks");

        NewsEntity entity = new NewsEntity();
        entity.setNewsLink(NEWS_LINK);
        entity.setRemarks("Updated remarks");
        entity.setHidden(false);

        when(repository.exists(NEWS_LINK)).thenReturn(true);
        when(repository.updateRemarks(NEWS_LINK, "Updated remarks")).thenReturn(1);
        when(repository.getNews(NEWS_LINK)).thenReturn(entity);

        // When
        NewsResponse response = service.updateNews(NEWS_LINK, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRemarks()).isEqualTo("Updated remarks");

        verify(repository).exists(NEWS_LINK);
        verify(repository).updateRemarks(NEWS_LINK, "Updated remarks");
        verify(repository).getNews(NEWS_LINK);
    }

    @Test
    void updateNews_notFoundOnCheck_throwsException() {
        // Given
        UpdateNewsRequest request = new UpdateNewsRequest();
        request.setRemarks("Updated remarks");

        when(repository.exists(NEWS_LINK)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> service.updateNews(NEWS_LINK, request))
                .isInstanceOf(NewsNotFoundException.class);

        verify(repository).exists(NEWS_LINK);
        verify(repository, never()).updateRemarks(any(), any());
    }

    @Test
    void updateNews_notFoundOnUpdate_throwsException() {
        // Given
        UpdateNewsRequest request = new UpdateNewsRequest();
        request.setRemarks("Updated remarks");

        when(repository.exists(NEWS_LINK)).thenReturn(true);
        when(repository.updateRemarks(NEWS_LINK, "Updated remarks")).thenReturn(0);

        // When/Then
        assertThatThrownBy(() -> service.updateNews(NEWS_LINK, request))
                .isInstanceOf(NewsNotFoundException.class);

        verify(repository).exists(NEWS_LINK);
        verify(repository).updateRemarks(NEWS_LINK, "Updated remarks");
        verify(repository, never()).getNews(any());
    }

    @Test
    void deleteNews_success() {
        // Given
        when(repository.exists(NEWS_LINK)).thenReturn(true);
        when(repository.delete(NEWS_LINK)).thenReturn(1);

        // When
        service.deleteNews(NEWS_LINK);

        // Then
        verify(repository).exists(NEWS_LINK);
        verify(repository).delete(NEWS_LINK);
    }

    @Test
    void deleteNews_notFoundOnCheck_throwsException() {
        // Given
        when(repository.exists(NEWS_LINK)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> service.deleteNews(NEWS_LINK))
                .isInstanceOf(NewsNotFoundException.class);

        verify(repository).exists(NEWS_LINK);
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteNews_notFoundOnDelete_throwsException() {
        // Given
        when(repository.exists(NEWS_LINK)).thenReturn(true);
        when(repository.delete(NEWS_LINK)).thenReturn(0);

        // When/Then
        assertThatThrownBy(() -> service.deleteNews(NEWS_LINK))
                .isInstanceOf(NewsNotFoundException.class);

        verify(repository).exists(NEWS_LINK);
        verify(repository).delete(NEWS_LINK);
    }

    @Test
    void getAllNews_returnsAll() {
        // Given
        NewsEntity entity1 = new NewsEntity();
        entity1.setNewsLink("https://example.com/news/1");
        entity1.setRemarks("News 1");
        entity1.setHidden(false);

        NewsEntity entity2 = new NewsEntity();
        entity2.setNewsLink("https://example.com/news/2");
        entity2.setRemarks("News 2");
        entity2.setHidden(true);

        when(repository.getAllNews()).thenReturn(Arrays.asList(entity1, entity2));

        // When
        List<NewsResponse> responses = service.getAllNews();

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getNewsLink()).isEqualTo("https://example.com/news/1");
        assertThat(responses.get(1).getNewsLink()).isEqualTo("https://example.com/news/2");

        verify(repository).getAllNews();
    }

    @Test
    void getAllVisibleNews_returnsOnlyVisible() {
        // Given
        NewsEntity entity1 = new NewsEntity();
        entity1.setNewsLink("https://example.com/news/1");
        entity1.setRemarks("News 1");
        entity1.setHidden(false);

        when(repository.getAllVisibleNews()).thenReturn(Arrays.asList(entity1));

        // When
        List<NewsResponse> responses = service.getAllVisibleNews();

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getNewsLink()).isEqualTo("https://example.com/news/1");
        assertThat(responses.get(0).isHidden()).isFalse();

        verify(repository).getAllVisibleNews();
    }

    @Test
    void hideSource_existingNews_hidesIt() {
        // Given
        when(repository.exists(NEWS_LINK)).thenReturn(true);
        when(repository.hideSource(NEWS_LINK)).thenReturn(1);

        // When
        NewsResponse response = service.hideSource(NEWS_LINK);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getNewsLink()).isEqualTo(NEWS_LINK);
        assertThat(response.isHidden()).isTrue();

        verify(repository).exists(NEWS_LINK);
        verify(repository, never()).create(any(), any());
        verify(repository).hideSource(NEWS_LINK);
    }

    @Test
    void hideSource_newNews_createsAndHides() {
        // Given
        when(repository.exists(NEWS_LINK)).thenReturn(false);
        when(repository.hideSource(NEWS_LINK)).thenReturn(1);

        // When
        NewsResponse response = service.hideSource(NEWS_LINK);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getNewsLink()).isEqualTo(NEWS_LINK);
        assertThat(response.isHidden()).isTrue();

        verify(repository).exists(NEWS_LINK);
        verify(repository).create(NEWS_LINK, null);
        verify(repository).hideSource(NEWS_LINK);
    }

    @Test
    void hideSource_hideFailsAfterCreation_throwsException() {
        // Given
        when(repository.exists(NEWS_LINK)).thenReturn(false);
        when(repository.hideSource(NEWS_LINK)).thenReturn(0);

        // When/Then
        assertThatThrownBy(() -> service.hideSource(NEWS_LINK))
                .isInstanceOf(NewsNotFoundException.class);

        verify(repository).exists(NEWS_LINK);
        verify(repository).create(NEWS_LINK, null);
        verify(repository).hideSource(NEWS_LINK);
    }

    @Test
    void unhideSource_success() {
        // Given
        NewsEntity entity = new NewsEntity();
        entity.setNewsLink(NEWS_LINK);
        entity.setRemarks(REMARKS);
        entity.setHidden(false);

        when(repository.exists(NEWS_LINK)).thenReturn(true);
        when(repository.unhideSource(NEWS_LINK)).thenReturn(1);
        when(repository.getNews(NEWS_LINK)).thenReturn(entity);

        // When
        NewsResponse response = service.unhideSource(NEWS_LINK);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getNewsLink()).isEqualTo(NEWS_LINK);
        assertThat(response.isHidden()).isFalse();

        verify(repository).exists(NEWS_LINK);
        verify(repository).unhideSource(NEWS_LINK);
        verify(repository).getNews(NEWS_LINK);
    }

    @Test
    void unhideSource_notFoundOnCheck_throwsException() {
        // Given
        when(repository.exists(NEWS_LINK)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> service.unhideSource(NEWS_LINK))
                .isInstanceOf(NewsNotFoundException.class);

        verify(repository).exists(NEWS_LINK);
        verify(repository, never()).unhideSource(any());
    }

    @Test
    void unhideSource_notFoundOnUnhide_throwsException() {
        // Given
        when(repository.exists(NEWS_LINK)).thenReturn(true);
        when(repository.unhideSource(NEWS_LINK)).thenReturn(0);

        // When/Then
        assertThatThrownBy(() -> service.unhideSource(NEWS_LINK))
                .isInstanceOf(NewsNotFoundException.class);

        verify(repository).exists(NEWS_LINK);
        verify(repository).unhideSource(NEWS_LINK);
        verify(repository, never()).getNews(any());
    }

    @Test
    void newsExists_returnsTrue() {
        // Given
        when(repository.exists(NEWS_LINK)).thenReturn(true);

        // When
        boolean exists = service.newsExists(NEWS_LINK);

        // Then
        assertThat(exists).isTrue();
        verify(repository).exists(NEWS_LINK);
    }

    @Test
    void newsExists_returnsFalse() {
        // Given
        when(repository.exists(NEWS_LINK)).thenReturn(false);

        // When
        boolean exists = service.newsExists(NEWS_LINK);

        // Then
        assertThat(exists).isFalse();
        verify(repository).exists(NEWS_LINK);
    }
}
