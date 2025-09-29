package database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        if (countryId == null || countryId.trim().isEmpty() ||
            partnerCountryId == null || partnerCountryId.trim().isEmpty() ||
            productId == null) {
            throw new DataAccessException("Missing country, partner or product id") {};
        }

        logger.info("Resolved values before DB operation - countryId: {}, partnerCountryId: {}, productId: {}, year: {}",
                countryId, partnerCountryId, productId, tariffRate.getYear());

        String checkSql = """
            SELECT COUNT(*) FROM TariffRates
            WHERE country_id = ? AND partner_country_id = ? AND product_id = ? AND year = ?
        """;

        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class,
            countryId, partnerCountryId, productId, tariffRate.getYear()
        );

        if (count != null && count > 0) {
            logger.debug("Record exists, updating tariff rate: {}", tariffRate);

            String updateSql = """
                UPDATE TariffRates SET 
                    rate = ?, 
                    last_updated = CURRENT_TIMESTAMP
                WHERE country_id = ? AND partner_country_id = ? AND product_id = ? AND year = ?
            """;

            int rowsUpdated = jdbcTemplate.update(updateSql,
                tariffRate.getRate(),
                countryId,
                partnerCountryId,
                productId,
                tariffRate.getYear()
            );

            logger.info("Updated {} rows for tariff rate: {}", rowsUpdated, tariffRate);

        } else {
            logger.debug("No existing record found, inserting new tariff rate: {}", tariffRate);

            String insertSql = """
                INSERT INTO TariffRates (
                    country_id, partner_country_id, product_id, 
                    year, rate, unit, in_effect, last_updated
                ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

            int rowsInserted = jdbcTemplate.update(insertSql,
                countryId,
                partnerCountryId,
                productId,
                tariffRate.getYear(),
                tariffRate.getRate(),
                "percent",
                1
            );

            logger.info("Inserted {} rows for tariff rate: {}", rowsInserted, tariffRate);
        }
    }
}