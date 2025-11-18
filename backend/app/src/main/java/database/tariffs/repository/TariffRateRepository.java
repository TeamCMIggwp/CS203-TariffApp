package database.tariffs.repository;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import database.tariffs.entity.TariffRateEntity;
import database.tariffs.mapper.TariffRateRowMapper;
import database.tariffs.util.TariffDataTransformer;

@Repository
public class TariffRateRepository implements ITariffRateRepository {
    private static final Logger logger = LoggerFactory.getLogger(TariffRateRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final TariffRateRowMapper rowMapper;

    public TariffRateRepository(
            @org.springframework.beans.factory.annotation.Qualifier("appJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = new TariffRateRowMapper();
    }

    @Override
    public boolean exists(String reporter, String partner, Integer product, String year) {
        try {
            String sql = """
                SELECT COUNT(*) FROM `wto_tariffs`.`TariffRates`
                WHERE `country_id` = ? AND `partner_country_id` = ?
                  AND `product_id` = ? AND `year` = ?
            """;

            Integer yearInt = TariffDataTransformer.parseYear(year);
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                reporter, partner, product, yearInt);

            return count != null && count > 0;
        } catch (DataAccessException e) {
            logger.error("Error checking tariff existence: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
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

            Integer yearInt = TariffDataTransformer.parseYear(year);

            return jdbcTemplate.queryForObject(sql, rowMapper, reporter, partner, product, yearInt);

        } catch (EmptyResultDataAccessException e) {
            logger.info("No tariff found");
            return null;
        } catch (DataAccessException e) {
            logger.error("Database error while retrieving tariff: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public java.util.List<TariffRateEntity> getAllTariffs() {
        try {
            logger.debug("Querying all tariffs");

            String sql = """
                SELECT `country_id`, `partner_country_id`, `product_id`, `year`, `rate`, `unit`
                FROM `wto_tariffs`.`TariffRates`
            """;

            return jdbcTemplate.query(sql, rowMapper);

        } catch (DataAccessException e) {
            logger.error("Database error while retrieving all tariffs: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void create(String reporter, String partner, Integer product, String year,
                      Double rate, String unit) {
        try {
            logger.info("Creating new tariff: reporter={}, partner={}, product={}, year={}, rate={}, unit={}",
                    reporter, partner, product, year, rate, unit);

            Integer yearInt = TariffDataTransformer.parseYear(year);
            BigDecimal rateDecimal = TariffDataTransformer.roundRate(rate);
            String unitValue = TariffDataTransformer.normalizeUnit(unit);

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

    @Override
    public int update(String reporter, String partner, Integer product, String year,
                     Double rate, String unit) {
        try {
            logger.info("Updating tariff: reporter={}, partner={}, product={}, year={}, rate={}, unit={}",
                    reporter, partner, product, year, rate, unit);

            Integer yearInt = TariffDataTransformer.parseYear(year);
            BigDecimal rateDecimal = TariffDataTransformer.roundRate(rate);
            String unitValue = TariffDataTransformer.normalizeUnit(unit);

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

    @Override
    public int delete(String reporter, String partner, Integer product, String year) {
        try {
            logger.info("Deleting tariff: reporter={}, partner={}, product={}, year={}",
                    reporter, partner, product, year);

            Integer yearInt = TariffDataTransformer.parseYear(year);

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