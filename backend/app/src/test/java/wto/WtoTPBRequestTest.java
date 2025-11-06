package wto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import wto.dto.WtoTPBRequest;

class WtoTPBRequestTest {

    @Test
    void toQuery_includesModeAndDefaultsToFullUnlessOverridden() {
        WtoTPBRequest r = new WtoTPBRequest();
        r.setIndicator("TP_B_0180");
        r.setReporter("702");
        r.setFormat("json");

        Map<String, String> q = r.toQuery();
        assertEquals("TP_B_0180", q.get("i"));
        assertEquals("702", q.get("r"));
        assertEquals("json", q.get("fmt"));
        assertEquals("full", q.get("mode")); // default in class

        r.setMode("compact");
        q = r.toQuery();
        assertEquals("compact", q.get("mode"));
    }
}
