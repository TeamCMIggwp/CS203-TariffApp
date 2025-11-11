package leaderboard.dto;

public class LeaderboardResponse {

    private Long id;
    private String name;
    private Integer score;

    public LeaderboardResponse() {
    }

    public LeaderboardResponse(Long id, String name, Integer score) {
        this.id = id;
        this.name = name;
        this.score = score;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getScore() {
        return score;
    }
}
