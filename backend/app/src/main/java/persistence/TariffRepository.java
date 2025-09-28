package persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TariffRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void updateTariffRate(TariffRate tariffRate) throws DataAccessException {
        String sql = """
            INSERT INTO TariffRates (
                country_id, partner_country_id, product_id, 
                year, rate, last_updated
            ) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE 
                rate = ?,
                last_updated = CURRENT_TIMESTAMP
            """;

        jdbcTemplate.update(sql,
            tariffRate.getCountryId(),
            tariffRate.getPartnerId(),
            tariffRate.getProductHsCode(),
            tariffRate.getYear(),
            tariffRate.getRate(),
            // Value for UPDATE clause
            tariffRate.getRate()
        );
    }
}
