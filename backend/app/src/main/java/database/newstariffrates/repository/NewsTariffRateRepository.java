package database.newstariffrates.repository;

import database.newstariffrates.entity.NewsTariffRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class NewsTariffRateRepository {
    private static final Logger logger = LoggerFactory.getLogger(NewsTariffRateRepository.class);

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("appJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * Check if a tariff rate already exists for a given news link
     */
    public boolean existsByNewsLink(String newsLink) {
        try {
            String sql = "SELECT COUNT(*) FROM TariffRates WHERE news_link = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, newsLink);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            logger.error("Error checking tariff rate existence: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Find tariff rate by news link
     */
    public Optional<NewsTariffRate> findByNewsLink(String newsLink) {
        try {
            String sql = "SELECT tariff_id, news_link, country_id, partner_country_id, product_id, " +
                        "tariff_type_id, year, rate, unit, last_updated, in_effect " +
                        "FROM TariffRates WHERE news_link = ?";

            NewsTariffRate tariffRate = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                NewsTariffRate tr = new NewsTariffRate();
                tr.setTariffId(rs.getInt("tariff_id"));
                tr.setNewsLink(rs.getString("news_link"));
                tr.setCountryId(rs.getString("country_id"));
                tr.setPartnerCountryId(rs.getString("partner_country_id"));
                tr.setProductId(rs.getInt("product_id"));
                tr.setTariffTypeId((Integer) rs.getObject("tariff_type_id"));
                tr.setYear(rs.getInt("year"));
                tr.setRate(rs.getDouble("rate"));
                tr.setUnit(rs.getString("unit"));
                tr.setLastUpdated(rs.getTimestamp("last_updated"));
                tr.setInEffect(rs.getBoolean("in_effect"));
                return tr;
            }, newsLink);

            return Optional.of(tariffRate);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            logger.error("Error finding tariff rate by news link: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Save new tariff rate
     */
    public NewsTariffRate save(NewsTariffRate tariffRate) {
        try {
            logger.info("Saving tariff rate for news: {}", tariffRate.getNewsLink());

            String sql = "INSERT INTO TariffRates (news_link, country_id, partner_country_id, product_id, " +
                        "tariff_type_id, year, rate, unit, last_updated, in_effect) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, tariffRate.getNewsLink());
                ps.setString(2, tariffRate.getCountryId());
                ps.setString(3, tariffRate.getPartnerCountryId());
                if (tariffRate.getProductId() != null) {
                    ps.setInt(4, tariffRate.getProductId());
                } else {
                    ps.setNull(4, java.sql.Types.INTEGER);
                }
                if (tariffRate.getTariffTypeId() != null) {
                    ps.setInt(5, tariffRate.getTariffTypeId());
                } else {
                    ps.setNull(5, java.sql.Types.INTEGER);
                }
                if (tariffRate.getYear() != null) {
                    ps.setInt(6, tariffRate.getYear());
                } else {
                    ps.setNull(6, java.sql.Types.INTEGER);
                }
                if (tariffRate.getRate() != null) {
                    ps.setDouble(7, tariffRate.getRate());
                } else {
                    ps.setNull(7, java.sql.Types.DOUBLE);
                }
                ps.setString(8, tariffRate.getUnit());
                ps.setBoolean(9, tariffRate.getInEffect() != null ? tariffRate.getInEffect() : true);
                return ps;
            }, keyHolder);

            // Set the generated ID
            Number key = keyHolder.getKey();
            if (key != null) {
                tariffRate.setTariffId(key.intValue());
            }

            logger.info("Tariff rate saved with ID: {}", tariffRate.getTariffId());
            return tariffRate;

        } catch (DataAccessException e) {
            logger.error("Error saving tariff rate: {}", e.getMessage(), e);
            throw e;
        }
    }
}
