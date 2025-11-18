package database.newstariffrates.mapper;

import database.newstariffrates.entity.NewsTariffRate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RowMapper for NewsTariffRate.
 * Follows Single Responsibility Principle - only handles mapping ResultSet to Entity.
 */
public class NewsTariffRateRowMapper implements RowMapper<NewsTariffRate> {

    @Override
    public NewsTariffRate mapRow(ResultSet rs, int rowNum) throws SQLException {
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
    }
}
