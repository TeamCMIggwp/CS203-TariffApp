package database.leaderboard.controller;

import database.leaderboard.dto.LeaderboardRequest;
import database.leaderboard.dto.LeaderboardResponse;
import database.leaderboard.service.LeaderboardService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final LeaderboardService service;

    public LeaderboardController(LeaderboardService service) {
        this.service = service;
    }

    /**
     * GET /leaderboard
     * Returns top 10 entries ordered by highest score.
     */
    @GetMapping
    public List<LeaderboardResponse> getTop10() {
        return service.getTop10();
    }

    /**
     * PUT /leaderboard
     * Body: { "name": "...", "score": 123 }
     * Creates or updates a leaderboard entry for that name.
     */
    @PutMapping
    public LeaderboardResponse submitScore(@RequestBody LeaderboardRequest request) {
        return service.submitScore(request);
    }
}
