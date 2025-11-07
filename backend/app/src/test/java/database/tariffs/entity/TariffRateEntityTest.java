package database.tariffs.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TariffRateEntityTest {

    @Test
    void settersAndGetters_shouldStoreAndReturnAllFields() {
        TariffRateEntity entity = new TariffRateEntity();

        entity.setCountryIsoNumeric("702");
        entity.setPartnerIsoNumeric("156");
        entity.setProductHsCode(100630);
        entity.setYear("2020");
        entity.setRate(7.5);
        entity.setUnit("%");

        assertEquals("702", entity.getCountryIsoNumeric());
        assertEquals("156", entity.getPartnerIsoNumeric());
        assertEquals(Integer.valueOf(100630), entity.getProductHsCode());
        assertEquals("2020", entity.getYear());
        assertEquals(7.5, entity.getRate());
        assertEquals("%", entity.getUnit());
    }

    @Test
    void toString_shouldIncludeAllKeyFields() {
        TariffRateEntity entity = new TariffRateEntity();
        entity.setCountryIsoNumeric("702");
        entity.setPartnerIsoNumeric("156");
        entity.setProductHsCode(100630);
        entity.setYear("2020");
        entity.setRate(7.5);
        entity.setUnit("%");

        String str = entity.toString();

        // Basic structure check
        assertNotNull(str);
        assertTrue(str.startsWith("TariffRateEntity{"));
        assertTrue(str.endsWith("}"));

        // Content checks aligned with TariffRateEntity.toString() implementation
        assertTrue(str.contains("countryIsoNumeric='702'"));
        assertTrue(str.contains("partnerIsoNumeric='156'"));
        assertTrue(str.contains("productHsCode=100630"));
        assertTrue(str.contains("year='2020'"));
        assertTrue(str.contains("rate=7.5"));
        assertTrue(str.contains("unit='%'"));
    }
}
