package database.tariffs.mapper;

import database.tariffs.entity.TariffRateEntity;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RowMapper for TariffRateEntity.
 * Follows Single Responsibility Principle - only handles mapping ResultSet to Entity.
 * Eliminates code duplication by centralizing row mapping logic.
 */
public class TariffRateRowMapper implements RowMapper<TariffRateEntity> {

    @Override
    public TariffRateEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        TariffRateEntity tariff = new TariffRateEntity();
        tariff.setCountryIsoNumeric(rs.getString("country_id"));
        tariff.setPartnerIsoNumeric(rs.getString("partner_country_id"));
        tariff.setProductHsCode(rs.getInt("product_id"));
        tariff.setYear(String.valueOf(rs.getInt("year")));
        tariff.setRate(rs.getDouble("rate"));
        tariff.setUnit(rs.getString("unit"));
        return tariff;
    }
}
