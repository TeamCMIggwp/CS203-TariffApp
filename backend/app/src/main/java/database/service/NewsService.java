package database.service;

import database.NewsRepository;
import database.NewsEntity;
import database.dto.CreateNewsRequest;
import database.dto.UpdateNewsRequest;
import database.dto.NewsResponse;
import database.exception.NewsAlreadyExistsException;
import database.exception.NewsNotFoundException;

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
        
        return new NewsResponse(request.getNewsLink(), request.getRemarks());
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
        
        if (rowsUpdated == 0) {
            throw new NewsNotFoundException(newsLink);
        }
        
        logger.info("Successfully updated news");
        
        return new NewsResponse(newsLink, request.getRemarks());
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
        
        return new NewsResponse(entity.getNewsLink(), entity.getRemarks());
    }
    
    /**
     * Get all news
     */
    public List<NewsResponse> getAllNews() {
        logger.info("Retrieving all news");
        
        List<NewsEntity> entities = repository.getAllNews();
        
        return entities.stream()
                .map(entity -> new NewsResponse(entity.getNewsLink(), entity.getRemarks()))
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
        
        if (rowsDeleted == 0) {
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
}