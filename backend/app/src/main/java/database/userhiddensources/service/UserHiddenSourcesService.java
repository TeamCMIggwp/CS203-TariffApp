package database.userhiddensources.service;

import database.userhiddensources.dto.HiddenSourceResponse;
import database.userhiddensources.entity.UserHiddenSourcesEntity;
import database.userhiddensources.repository.UserHiddenSourcesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserHiddenSourcesService {
    private static final Logger logger = LoggerFactory.getLogger(UserHiddenSourcesService.class);
    private static final int NO_ROWS_AFFECTED = 0;

    @Autowired
    private UserHiddenSourcesRepository repository;

    /**
     * Hide a source for a regular user
     */
    @Transactional
    public HiddenSourceResponse hideSourceForUser(String userId, String newsLink) {
        logger.info("Hiding source for user: userId={}, newsLink={}", userId, newsLink);

        repository.hideSource(userId, newsLink);

        return new HiddenSourceResponse(newsLink, java.time.LocalDateTime.now());
    }

    /**
     * Unhide a source for a regular user
     */
    @Transactional
    public void unhideSourceForUser(String userId, String newsLink) {
        logger.info("Unhiding source for user: userId={}, newsLink={}", userId, newsLink);

        int rowsDeleted = repository.unhideSource(userId, newsLink);

        if (rowsDeleted == NO_ROWS_AFFECTED) {
            logger.warn("No hidden source found for user: userId={}, newsLink={}", userId, newsLink);
        }
    }

    /**
     * Unhide all sources for a regular user
     */
    @Transactional
    public int unhideAllSourcesForUser(String userId) {
        logger.info("Unhiding all sources for user: {}", userId);

        return repository.unhideAllSources(userId);
    }

    /**
     * Get all hidden sources for a user
     */
    public List<HiddenSourceResponse> getHiddenSourcesForUser(String userId) {
        logger.info("Retrieving hidden sources for user: {}", userId);

        List<UserHiddenSourcesEntity> entities = repository.getAllHiddenSourcesByUser(userId);

        return entities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Check if user has hidden a specific source
     */
    public boolean isSourceHiddenByUser(String userId, String newsLink) {
        return repository.isHiddenByUser(userId, newsLink);
    }

    /**
     * Map UserHiddenSourcesEntity to HiddenSourceResponse
     */
    private HiddenSourceResponse mapToResponse(UserHiddenSourcesEntity entity) {
        return new HiddenSourceResponse(entity.getNewsLink(), entity.getHiddenAt());
    }
}
