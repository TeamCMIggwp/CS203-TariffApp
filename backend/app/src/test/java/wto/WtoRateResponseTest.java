package wto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import wto.dto.WTORateResponse;

class WTORateResponseTest {

    @Test
    void gettersAndSetters_roundTripValues() {
        WTORateResponse r = new WTORateResponse();
        r.setCode("HS_P_0070");
        r.setName("Trade value of product");
        r.setCategoryCode("CAT");
        r.setCategoryLabel("Category");
        r.setSubcategoryCode("SUB");
        r.setSubcategoryLabel("Subcategory");
        r.setUnitCode("USD");
        r.setUnitLabel("US Dollars");
        r.setStartYear(1990);
        r.setEndYear(2024);
        r.setHasMetadata("Y");
        r.setNumberDecimals(2);
        r.setNumberDatapoints(12345L);
        r.setUpdateFrequency("Annual");
        r.setDescription("Some description");
        r.setSortOrder(10);

        assertEquals("HS_P_0070", r.getCode());
        assertEquals("Trade value of product", r.getName());
        assertEquals("CAT", r.getCategoryCode());
        assertEquals("Category", r.getCategoryLabel());
        assertEquals("SUB", r.getSubcategoryCode());
        assertEquals("Subcategory", r.getSubcategoryLabel());
        assertEquals("USD", r.getUnitCode());
        assertEquals("US Dollars", r.getUnitLabel());
        assertEquals(1990, r.getStartYear());
        assertEquals(2024, r.getEndYear());
        assertEquals("Y", r.getHasMetadata());
        assertEquals(2, r.getNumberDecimals());
        assertEquals(12345L, r.getNumberDatapoints());
        assertEquals("Annual", r.getUpdateFrequency());
        assertEquals("Some description", r.getDescription());
        assertEquals(10, r.getSortOrder());
    }
}
