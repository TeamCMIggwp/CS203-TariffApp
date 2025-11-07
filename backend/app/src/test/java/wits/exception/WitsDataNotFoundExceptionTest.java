package wits.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WitsDataNotFoundExceptionTest {

    @Test
    void constructor_setsMessageAndAllContextFields() {
        WitsDataNotFoundException ex =
            new WitsDataNotFoundException("702", "156", 100630, "2019");

        // Message should include all context parts
        String msg = ex.getMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("702"));
        assertTrue(msg.contains("156"));
        assertTrue(msg.contains("100630"));
        assertTrue(msg.contains("2019"));

        // And getters expose the same context cleanly
        assertEquals("702", ex.getReporter());
        assertEquals("156", ex.getPartner());
        assertEquals(Integer.valueOf(100630), ex.getProduct());
        assertEquals("2019", ex.getYear());
    }
}
