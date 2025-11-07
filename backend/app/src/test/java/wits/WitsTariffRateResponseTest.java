package wits;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import wits.dto.WitsTariffRateResponse;

class WitsTariffRateResponseTest {

    @Test
    void constructor_setsFields_andGettersReturn() {
        WitsTariffRateResponse r =
            new WitsTariffRateResponse("840","000",100630,"2020","1.1","2.2","1.5");

        assertEquals("840", r.getReporter());
        assertEquals("000", r.getPartner());
        assertEquals(100630, r.getProduct());
        assertEquals("2020", r.getYear());
        assertEquals("1.1", r.getMinRate());
        assertEquals("2.2", r.getMaxRate());
        assertEquals("1.5", r.getAvgRate());
        // Match actual implementation: default source is "WITS"
        assertEquals("WITS", r.getSource());
        assertTrue(r.isDataAvailable());
        assertNotNull(r.getTimestamp());
    }

    @Test
    void setters_overrideValues() {
        WitsTariffRateResponse r =
            new WitsTariffRateResponse("840","000",100630,"2020",null,null,null);

        r.setMinRate("0.5");
        r.setMaxRate("3.5");
        r.setAvgRate("1.2");
        r.setSource("CustomSource");
        r.setDataAvailable(false);
        LocalDateTime now = LocalDateTime.now();
        r.setTimestamp(now);

        assertEquals("0.5", r.getMinRate());
        assertEquals("3.5", r.getMaxRate());
        assertEquals("1.2", r.getAvgRate());
        assertEquals("CustomSource", r.getSource());
        assertFalse(r.isDataAvailable());
        assertEquals(now, r.getTimestamp());
    }
}
