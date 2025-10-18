package database;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TariffRateRepository {
    private static final Logger logger = LoggerFactory.getLogger(TariffRateRepository.class);

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("appJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * Check if tariff exists
     */
    public boolean exists(String reporter, String partner, Integer product, String year) {
        try {
            String sql = """
                SELECT COUNT(*) FROM `wto_tariffs`.`TariffRates`
                WHERE `country_id` = ? AND `partner_country_id` = ? 
                  AND `product_id` = ? AND `year` = ?
            """;
            
            Integer yearInt = Integer.valueOf(year);
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                reporter, partner, product, yearInt);
            
            return count != null && count > 0;
        } catch (DataAccessException e) {
            logger.error("Error checking tariff existence: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get tariff by composite key
     */
    public TariffRateEntity getTariff(String reporter, String partner, Integer product, String year) {
        try {
            logger.debug("Querying tariff for: reporter={}, partner={}, product={}, year={}",
                    reporter, partner, product, year);

            String sql = """
                SELECT `country_id`, `partner_country_id`, `product_id`, `year`, `rate`, `unit`
                FROM `wto_tariffs`.`TariffRates`
                WHERE `country_id` = ? AND `partner_country_id` = ? 
                  AND `product_id` = ? AND `year` = ?
            """;
            
            Integer yearInt = Integer.valueOf(year);
            
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                TariffRateEntity tariff = new TariffRateEntity();
                tariff.setCountryIsoNumeric(rs.getString("country_id"));
                tariff.setPartnerIsoNumeric(rs.getString("partner_country_id"));
                tariff.setProductHsCode(rs.getInt("product_id"));
                tariff.setYear(String.valueOf(rs.getInt("year")));
                tariff.setRate(rs.getDouble("rate"));
                tariff.setUnit(rs.getString("unit"));
                return tariff;
            }, reporter, partner, product, yearInt);

        } catch (EmptyResultDataAccessException e) {
            logger.info("No tariff found");
            return null;
        } catch (DataAccessException e) {
            logger.error("Database error while retrieving tariff: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get all tariffs
     */
    public java.util.List<TariffRateEntity> getAllTariffs() {
        try {
            logger.debug("Querying all tariffs");

            String sql = """
                SELECT `country_id`, `partner_country_id`, `product_id`, `year`, `rate`, `unit`
                FROM `wto_tariffs`.`TariffRates`
            """;
            
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                TariffRateEntity tariff = new TariffRateEntity();
                tariff.setCountryIsoNumeric(rs.getString("country_id"));
                tariff.setPartnerIsoNumeric(rs.getString("partner_country_id"));
                tariff.setProductHsCode(rs.getInt("product_id"));
                tariff.setYear(String.valueOf(rs.getInt("year")));
                tariff.setRate(rs.getDouble("rate"));
                tariff.setUnit(rs.getString("unit"));
                return tariff;
            });

        } catch (DataAccessException e) {
            logger.error("Database error while retrieving all tariffs: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Create new tariff - throws exception if already exists
     */
    public void create(String reporter, String partner, Integer product, String year, 
                      Double rate, String unit) {
        try {
            logger.info("Creating new tariff: reporter={}, partner={}, product={}, year={}, rate={}, unit={}",
                    reporter, partner, product, year, rate, unit);
            
            // Convert year to Integer for DB
            Integer yearInt = Integer.valueOf(year);
            
            // Round rate to 3 decimal places for DECIMAL(6,3)
            BigDecimal rateDecimal = BigDecimal.valueOf(rate).setScale(3, RoundingMode.HALF_UP);
            
            // Default unit if null
            String unitValue = (unit == null || unit.trim().isEmpty()) ? "percent" : unit;
            if (unitValue.length() > 20) {
                unitValue = unitValue.substring(0, 20);
            }
            
            String sql = """
                INSERT INTO `wto_tariffs`.`TariffRates` (
                    `country_id`, `partner_country_id`, `product_id`, `year`, `rate`, `unit`
                ) VALUES (?, ?, ?, ?, ?, ?)
            """;
            
            int rowsInserted = jdbcTemplate.update(sql,
                reporter, partner, product, yearInt, rateDecimal, unitValue);
            
            logger.info("Successfully created tariff, rows inserted: {}", rowsInserted);
            
        } catch (DataAccessException e) {
            logger.error("Error creating tariff: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update existing tariff - does NOT check if exists (caller's responsibility)
     */
    public int update(String reporter, String partner, Integer product, String year,
                     Double rate, String unit) {
        try {
            logger.info("Updating tariff: reporter={}, partner={}, product={}, year={}, rate={}, unit={}",
                    reporter, partner, product, year, rate, unit);
            
            // Convert year to Integer for DB
            Integer yearInt = Integer.valueOf(year);
            
            // Round rate to 3 decimal places
            BigDecimal rateDecimal = BigDecimal.valueOf(rate).setScale(3, RoundingMode.HALF_UP);
            
            // Default unit if null
            String unitValue = (unit == null || unit.trim().isEmpty()) ? "percent" : unit;
            if (unitValue.length() > 20) {
                unitValue = unitValue.substring(0, 20);
            }
            
            String sql = """
                UPDATE `wto_tariffs`.`TariffRates` 
                SET `rate` = ?, `unit` = ?
                WHERE `country_id` = ? AND `partner_country_id` = ? 
                  AND `product_id` = ? AND `year` = ?
            """;
            
            int rowsUpdated = jdbcTemplate.update(sql,
                rateDecimal, unitValue, reporter, partner, product, yearInt);
            
            logger.info("Updated {} rows", rowsUpdated);
            return rowsUpdated;
            
        } catch (DataAccessException e) {
            logger.error("Error updating tariff: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Delete tariff by composite key
     */
    public int delete(String reporter, String partner, Integer product, String year) {
        try {
            logger.info("Deleting tariff: reporter={}, partner={}, product={}, year={}",
                    reporter, partner, product, year);
            
            Integer yearInt = Integer.valueOf(year);
            
            String sql = """
                DELETE FROM `wto_tariffs`.`TariffRates`
                WHERE `country_id` = ? AND `partner_country_id` = ? 
                  AND `product_id` = ? AND `year` = ?
            """;
            
            int rowsDeleted = jdbcTemplate.update(sql, reporter, partner, product, yearInt);
            
            logger.info("Deleted {} rows", rowsDeleted);
            return rowsDeleted;
            
        } catch (DataAccessException e) {
            logger.error("Error deleting tariff: {}", e.getMessage(), e);
            throw e;
        }
    }
}