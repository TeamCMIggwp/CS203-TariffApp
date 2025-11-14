package database.news.service;

import database.news.dto.CreateNewsRequest;
import database.news.dto.NewsResponse;
import database.news.dto.UpdateNewsRequest;
import database.news.entity.NewsEntity;
import database.news.exception.NewsAlreadyExistsException;
import database.news.exception.NewsNotFoundException;
import database.news.repository.NewsRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsService {
    private static final Logger logger = LoggerFactory.getLogger(NewsService.class);
    private static final int NO_ROWS_AFFECTED = 0;

    @Autowired
    private NewsRepository repository;
    
    /**
     * Create new news - throws NewsAlreadyExistsException if exists
     */
    @Transactional
    public NewsResponse createNews(CreateNewsRequest request) {
        logger.info("Creating news for: newsLink={}", request.getNewsLink());
        
        // Check if already exists
        if (repository.exists(request.getNewsLink())) {
            throw new NewsAlreadyExistsException(request.getNewsLink());
        }
        
        // Create new news
        repository.create(request.getNewsLink(), request.getRemarks());

        logger.info("Successfully created news");

        return new NewsResponse(request.getNewsLink(), request.getRemarks(), false);
    }
    
    /**
     * Update existing news remarks - throws NewsNotFoundException if not exists
     */
    @Transactional
    public NewsResponse updateNews(String newsLink, UpdateNewsRequest request) {
        logger.info("Updating news for: newsLink={}", newsLink);
        
        // Check if exists
        if (!repository.exists(newsLink)) {
            throw new NewsNotFoundException(newsLink);
        }
        
        // Update news
        int rowsUpdated = repository.updateRemarks(newsLink, request.getRemarks());

        if (rowsUpdated == NO_ROWS_AFFECTED) {
            throw new NewsNotFoundException(newsLink);
        }

        logger.info("Successfully updated news");

        // Fetch the entity to get the isHidden value
        NewsEntity entity = repository.getNews(newsLink);
        return new NewsResponse(newsLink, request.getRemarks(), entity.isHidden());
    }
    
    /**
     * Get news by link
     */
    public NewsResponse getNews(String newsLink) {
        logger.info("Retrieving news for: newsLink={}", newsLink);

        NewsEntity entity = repository.getNews(newsLink);

        if (entity == null) {
            throw new NewsNotFoundException(newsLink);
        }

        return new NewsResponse(entity.getNewsLink(), entity.getRemarks(), entity.isHidden());
    }
    
    /**
     * Get all news
     */
    public List<NewsResponse> getAllNews() {
        logger.info("Retrieving all news");

        List<NewsEntity> entities = repository.getAllNews();

        return entities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all visible news (not hidden)
     */
    public List<NewsResponse> getAllVisibleNews() {
        logger.info("Retrieving all visible news");

        List<NewsEntity> entities = repository.getAllVisibleNews();

        return entities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Delete news by link
     */
    @Transactional
    public void deleteNews(String newsLink) {
        logger.info("Deleting news for: newsLink={}", newsLink);
        
        // Check if exists
        if (!repository.exists(newsLink)) {
            throw new NewsNotFoundException(newsLink);
        }
        
        int rowsDeleted = repository.delete(newsLink);

        if (rowsDeleted == NO_ROWS_AFFECTED) {
            throw new NewsNotFoundException(newsLink);
        }
        
        logger.info("Successfully deleted news");
    }
    
    /**
     * Check if news exists
     */
    public boolean newsExists(String newsLink) {
        return repository.exists(newsLink);
    }

    /**
     * Hide a news source - if it doesn't exist, create it as hidden
     */
    @Transactional
    public NewsResponse hideSource(String newsLink) {
        logger.info("Hiding news source: newsLink={}", newsLink);

        if (!repository.exists(newsLink)) {
            // If news doesn't exist, create it as hidden with empty remarks
            repository.create(newsLink, null);
        }

        int rowsUpdated = repository.hideSource(newsLink);

        if (rowsUpdated == NO_ROWS_AFFECTED) {
            throw new NewsNotFoundException(newsLink);
        }

        logger.info("Successfully hidden news source");

        return new NewsResponse(newsLink, null, true);
    }

    /**
     * Unhide a news source - throws NewsNotFoundException if not exists
     */
    @Transactional
    public NewsResponse unhideSource(String newsLink) {
        logger.info("Unhiding news source: newsLink={}", newsLink);

        if (!repository.exists(newsLink)) {
            throw new NewsNotFoundException(newsLink);
        }

        int rowsUpdated = repository.unhideSource(newsLink);

        if (rowsUpdated == NO_ROWS_AFFECTED) {
            throw new NewsNotFoundException(newsLink);
        }

        logger.info("Successfully unhidden news source");

        NewsEntity entity = repository.getNews(newsLink);
        return new NewsResponse(entity.getNewsLink(), entity.getRemarks(), entity.isHidden());
    }

    /**
     * Map NewsEntity to NewsResponse
     */
    private NewsResponse mapToResponse(NewsEntity entity) {
        return new NewsResponse(entity.getNewsLink(), entity.getRemarks(), entity.isHidden());
    }
}