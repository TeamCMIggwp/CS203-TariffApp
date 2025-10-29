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

    @Autowired
    private UserHiddenSourcesRepository repository;

    /**
     * Hide a source for a regular user
     */
    @Transactional
    public HiddenSourceResponse hideSourceForUser(String userEmail, String newsLink) {
        logger.info("Hiding source for user: userEmail={}, newsLink={}", userEmail, newsLink);

        repository.hideSource(userEmail, newsLink);

        return new HiddenSourceResponse(newsLink, java.time.LocalDateTime.now());
    }

    /**
     * Unhide a source for a regular user
     */
    @Transactional
    public void unhideSourceForUser(String userEmail, String newsLink) {
        logger.info("Unhiding source for user: userEmail={}, newsLink={}", userEmail, newsLink);

        int rowsDeleted = repository.unhideSource(userEmail, newsLink);

        if (rowsDeleted == 0) {
            logger.warn("No hidden source found for user: userEmail={}, newsLink={}", userEmail, newsLink);
        }
    }

    /**
     * Unhide all sources for a regular user
     */
    @Transactional
    public int unhideAllSourcesForUser(String userEmail) {
        logger.info("Unhiding all sources for user: {}", userEmail);

        return repository.unhideAllSources(userEmail);
    }

    /**
     * Get all hidden sources for a user
     */
    public List<HiddenSourceResponse> getHiddenSourcesForUser(String userEmail) {
        logger.info("Retrieving hidden sources for user: {}", userEmail);

        List<UserHiddenSourcesEntity> entities = repository.getAllHiddenSourcesByUser(userEmail);

        return entities.stream()
                .map(entity -> new HiddenSourceResponse(entity.getNewsLink(), entity.getHiddenAt()))
                .collect(Collectors.toList());
    }

    /**
     * Check if user has hidden a specific source
     */
    public boolean isSourceHiddenByUser(String userEmail, String newsLink) {
        return repository.isHiddenByUser(userEmail, newsLink);
    }
}
