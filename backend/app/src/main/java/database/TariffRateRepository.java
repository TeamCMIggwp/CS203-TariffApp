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

    public Integer getProductIdByHsCode(Integer hsCode) {
        try {
            String sql = "SELECT product_id FROM Products WHERE hs_code = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, hsCode);
        } catch (EmptyResultDataAccessException e) {
            logger.warn("No product found with HS code: {}", hsCode);
            return null;
        }
    }

    public Integer getCountryIdByIsoNumeric(int isoNumeric) {
        try {
            String sql = "SELECT country_id FROM Country WHERE iso_numeric = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, isoNumeric);
        } catch (EmptyResultDataAccessException e) {
            logger.warn("No country found with ISO numeric: {}", isoNumeric);
            return null;
        }
    }

    public Double getTariffRate(Integer reporterCountryId, Integer partnerCountryId, Integer productId, String year) {
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

        Integer countryId = tariffRate.getCountryId();
        Integer partnerCountryId = tariffRate.getPartnerId();

        if (countryId == null || partnerCountryId == null) {
            throw new DataAccessException("Country or partner country ID is null") {};
        }

        Integer productId = getProductIdByHsCode(tariffRate.getProductHsCode());
        if (productId == null) {
            throw new DataAccessException("No product found with HS code: " + tariffRate.getProductHsCode()) {};
        }

        logger.info("Resolved IDs before DB operation - countryId: {}, partnerCountryId: {}, productId: {}, year: {}",
                countryId, partnerCountryId, productId, tariffRate.getYear());

        String checkSql = """
            SELECT COUNT(*) FROM TariffRates
            WHERE country_id = ? AND partner_country_id = ? AND product_id = ? AND year = ?
        """;

        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class,
            countryId,
            partnerCountryId,
            productId,
            tariffRate.getYear());

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
                tariffRate.getYear());

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