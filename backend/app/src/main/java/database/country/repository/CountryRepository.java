package database.country.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import database.country.entity.Country;

import java.util.List;

@Repository
public class CountryRepository {
    private static final Logger logger = LoggerFactory.getLogger(CountryRepository.class);

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("appJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * Get all countries ordered by country name
     */
    public List<Country> getAllCountries() {
        try {
            logger.debug("Querying all countries");

            String sql = "SELECT iso_numeric, country_name, region, last_updated FROM Country ORDER BY country_name ASC";

            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Country country = new Country();
                country.setIsoNumeric(rs.getString("iso_numeric"));
                country.setCountryName(rs.getString("country_name"));
                country.setRegion(rs.getString("region"));
                country.setLastUpdated(rs.getTimestamp("last_updated"));
                return country;
            });

        } catch (DataAccessException e) {
            logger.error("Database error while retrieving all countries: {}", e.getMessage(), e);
            throw e;
        }
    }
}
