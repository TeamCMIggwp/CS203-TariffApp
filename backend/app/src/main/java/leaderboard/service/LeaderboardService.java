package leaderboard.service;

import leaderboard.dto.LeaderboardRequest;
import leaderboard.dto.LeaderboardResponse;
import leaderboard.entity.LeaderboardEntryEntity;
import leaderboard.repository.LeaderboardRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    private static final Logger logger = LoggerFactory.getLogger(LeaderboardService.class);

    private final LeaderboardRepository repository;

    public LeaderboardService(LeaderboardRepository repository) {
        this.repository = repository;
    }

    /**
     * GET: Return top 10 results with highest score.
     */
    public List<LeaderboardResponse> getTop10() {
        logger.info("Retrieving top 10 leaderboard entries");

        List<LeaderboardEntryEntity> entries = repository.getTop10();

        return entries.stream()
                .map(e -> new LeaderboardResponse(e.getId(), e.getName(), e.getScore()))
                .collect(Collectors.toList());
    }

    /**
     * PUT: Upsert by name.
     * - If name exists: update score.
     * - If not: create new record.
     */
    @Transactional
    public LeaderboardResponse submitScore(LeaderboardRequest request) {
        logger.info("Submitting score for name={}, score={}", request.getName(), request.getScore());

        // Try update existing
        int updatedRows = repository.update(request.getName(), request.getScore());

        if (updatedRows == 0) {
            logger.info("No existing record for name={}, creating new entry", request.getName());
            repository.create(request.getName(), request.getScore());
        }

        // Fetch the latest state
        LeaderboardEntryEntity saved = repository.findByName(request.getName())
                .orElseThrow(() -> {
                    logger.error("Failed to retrieve leaderboard entry after save for name={}", request.getName());
                    return new RuntimeException("Failed to save leaderboard entry");
                });

        logger.info("Saved leaderboard entry: id={}, name={}, score={}",
                saved.getId(), saved.getName(), saved.getScore());

        return new LeaderboardResponse(saved.getId(), saved.getName(), saved.getScore());
    }
}
