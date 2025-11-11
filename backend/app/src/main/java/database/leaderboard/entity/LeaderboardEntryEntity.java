package database.leaderboard.entity;

public class LeaderboardEntryEntity {

    private Long id;
    private String name;
    private Integer score;

    public LeaderboardEntryEntity() {
    }

    public LeaderboardEntryEntity(Long id, String name, Integer score) {
        this.id = id;
        this.name = name;
        this.score = score;
    }

    // Getters & setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
