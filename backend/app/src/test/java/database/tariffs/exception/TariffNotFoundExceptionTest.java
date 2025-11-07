package database.tariffs.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TariffNotFoundExceptionTest {

    @Test
    void constructor_setsMessageAndAllFields() {
        // Arrange
        String reporter = "702";
        String partner = "156";
        Integer product = 100630;
        String year = "2021";

        // Act
        TariffNotFoundException ex =
                new TariffNotFoundException(reporter, partner, product, year);

        // Assert: message format
        String msg = ex.getMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("Tariff not found for"));
        assertTrue(msg.contains("reporter=702"));
        assertTrue(msg.contains("partner=156"));
        assertTrue(msg.contains("product=100630"));
        assertTrue(msg.contains("year=2021"));

        // Assert: fields via getters
        assertEquals(reporter, ex.getReporter());
        assertEquals(partner, ex.getPartner());
        assertEquals(product, ex.getProduct());
        assertEquals(year, ex.getYear());
    }
}
