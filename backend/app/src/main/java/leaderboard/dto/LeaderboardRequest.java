package leaderboard.dto;

public class LeaderboardRequest {

    private String name;
    private Integer score;

    public LeaderboardRequest() {
    }

    public LeaderboardRequest(String name, Integer score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}
