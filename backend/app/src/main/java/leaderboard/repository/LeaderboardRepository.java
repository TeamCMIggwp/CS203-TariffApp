package leaderboard.repository;

import leaderboard.entity.LeaderboardEntryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class LeaderboardRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<LeaderboardEntryEntity> rowMapper = (rs, rowNum) ->
            new LeaderboardEntryEntity(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getInt("score")
            );

    /**
     * Get top 10 by highest score.
     */
    public List<LeaderboardEntryEntity> getTop10() {
        String sql = "SELECT id, name, score FROM leaderboard " +
                     "ORDER BY score DESC, id ASC " +
                     "LIMIT 10";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Find by name (for upsert logic).
     */
    public Optional<LeaderboardEntryEntity> findByName(String name) {
        String sql = "SELECT id, name, score FROM leaderboard WHERE name = ?";
        List<LeaderboardEntryEntity> list = jdbcTemplate.query(sql, rowMapper, name);
        return list.stream().findFirst();
    }

    /**
     * Create new entry.
     */
    public int create(String name, Integer score) {
        String sql = "INSERT INTO leaderboard (name, score) VALUES (?, ?)";
        return jdbcTemplate.update(sql, name, score);
    }

    /**
     * Update existing entry by name.
     */
    public int update(String name, Integer score) {
        String sql = "UPDATE leaderboard SET score = ? WHERE name = ?";
        return jdbcTemplate.update(sql, score, name);
    }
}
