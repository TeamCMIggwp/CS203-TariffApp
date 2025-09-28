package persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class TariffRepository {
    private static final Logger logger = LoggerFactory.getLogger(TariffRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void updateTariffRate(TariffRate tariffRate) throws DataAccessException {
        logger.debug("Checking existence of tariff rate: {}", tariffRate);

        String checkSql = """
            SELECT COUNT(*) FROM TariffRates
            WHERE country_id = ? AND partner_country_id = ? AND product_id = ? AND year = ?
        """;

        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class,
            tariffRate.getCountryId(),
            tariffRate.getPartnerId(),
            tariffRate.getProductHsCode(),
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
                tariffRate.getCountryId(),
                tariffRate.getPartnerId(),
                tariffRate.getProductHsCode(),
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
                tariffRate.getCountryId(),
                tariffRate.getPartnerId(),
                tariffRate.getProductHsCode(),
                tariffRate.getYear(),
                tariffRate.getRate(),
                "percent",  // unit
                1           // 1 for true
            );

            logger.info("Inserted {} rows for tariff rate: {}", rowsInserted, tariffRate);
        }
    }
}


