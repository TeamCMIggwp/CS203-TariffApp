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
    private JdbcTemplate jdbcTemplate;

    public Double getTariffRate(String reporterCountryId, String partnerCountryId, Integer productId, String year) {
        try {
            logger.debug("Querying tariff rate for: countryId={}, partnerCountryId={}, productId={}, year={}",
                    reporterCountryId, partnerCountryId, productId, year);

            String sql = """
                SELECT rate FROM TariffRates 
                WHERE country_id = ? AND partner_country_id = ? AND product_id = ? AND year = ?
            """;

            Double rate = jdbcTemplate.queryForObject(sql, Double.class,
                reporterCountryId, partnerCountryId, productId, year);

            logger.debug("Found tariff rate: {} for countryId={}, partnerCountryId={}, productId={}, year={}",
                    rate, reporterCountryId, partnerCountryId, productId, year);

            return rate;

        } catch (EmptyResultDataAccessException e) {
            logger.info("No tariff rate found for countryId={}, partnerCountryId={}, productId={}, year={}",
                    reporterCountryId, partnerCountryId, productId, year);
            return null;
        } catch (DataAccessException e) {
            logger.error("Database error while retrieving tariff rate: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void updateTariffRate(TariffRateEntity tariffRate) throws DataAccessException {
        logger.debug("Checking existence of tariff rate: {}", tariffRate);

        String countryId = tariffRate.getCountryIsoNumeric();          // char(3) expected
        String partnerCountryId = tariffRate.getPartnerIsoNumeric();  // char(3) expected
        Integer productId = tariffRate.getProductHsCode();            // codeUnique (int)
        String yearStr = tariffRate.getYear();                        // stored as INT in DB
        Double rateDouble = tariffRate.getRate();                     // DECIMAL(6,3) in DB
        String unit = tariffRate.getUnit();                           // VARCHAR(20) in DB

        if (countryId == null || countryId.trim().isEmpty() ||
            partnerCountryId == null || partnerCountryId.trim().isEmpty() ||
            productId == null || yearStr == null || yearStr.trim().isEmpty() || rateDouble == null) {
            throw new DataAccessException("Missing country, partner, product, year or rate") {};
        }

        // Default unit if not provided
        if (unit == null || unit.trim().isEmpty()) {
            unit = "percent";
        }
        // Trim to VARCHAR(20) safe length
        if (unit.length() > 20) {
            unit = unit.substring(0, 20);
        }

        logger.info("Resolved values before DB operation - countryId: {}, partnerCountryId: {}, productId: {}, year: {}, unit: {}",
                countryId, partnerCountryId, productId, yearStr, unit);

        // Ensure year is numeric to match DB column type (INT)
        Integer year;
        try {
            year = Integer.valueOf(yearStr);
        } catch (NumberFormatException ex) {
            throw new DataAccessException("Invalid year format: " + yearStr) {};
        }

        // Ensure rate fits DECIMAL(6,3): round to 3 decimal places
        BigDecimal rate = BigDecimal.valueOf(rateDouble).setScale(3, RoundingMode.HALF_UP);

        String checkSql = """
            SELECT COUNT(*) FROM TariffRates
            WHERE country_id = ? AND partner_country_id = ? AND product_id = ? AND year = ?
        """;

        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class,
            countryId, partnerCountryId, productId, year);

        if (count != null && count > 0) {
            logger.debug("Record exists, updating tariff rate: {}", tariffRate);

            String updateSql = """
                UPDATE TariffRates SET 
                    rate = ?,
                    unit = ?
                WHERE country_id = ? AND partner_country_id = ? AND product_id = ? AND year = ?
            """;

            int rowsUpdated = jdbcTemplate.update(updateSql,
                rate,
                unit,
                countryId,
                partnerCountryId,
                productId,
                year
            );

            logger.info("Updated {} rows for tariff rate: {}", rowsUpdated, tariffRate);

        } else {
            logger.debug("No existing record found, inserting new tariff rate: {}", tariffRate);

            String insertSql = """
                INSERT INTO TariffRates (
                    country_id, partner_country_id, product_id, year, rate, unit
                ) VALUES (?, ?, ?, ?, ?, ?)
            """;

            int rowsInserted = jdbcTemplate.update(insertSql,
                countryId,
                partnerCountryId,
                productId,
                year,
                rate,
                unit
            );

            logger.info("Inserted {} rows for tariff rate: {}", rowsInserted, tariffRate);
        }
    }
}