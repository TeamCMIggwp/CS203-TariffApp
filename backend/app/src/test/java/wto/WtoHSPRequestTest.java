package wto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import wto.dto.WtoHSPRequest;

class WtoHSPRequestTest {

    @Test
    void toQuery_includesAllRequired_andOptionalModeOnlyWhenPresent() {
        WtoHSPRequest r = new WtoHSPRequest();
        r.setIndicator("HS_P_0070");
        r.setReporter("702");
        r.setPartner("840");         // optional
        r.setProductCode("100630");
        r.setPeriod("2018-2020");
        r.setFormat("json");
        r.setMode("compact");        // optional

        Map<String, String> q = r.toQuery();
        assertEquals("HS_P_0070", q.get("i"));
        assertEquals("702", q.get("r"));
        assertEquals("840", q.get("p"));
        assertEquals("100630", q.get("pc"));
        assertEquals("2018-2020", q.get("ps"));
        assertEquals("json", q.get("fmt"));
        assertEquals("compact", q.get("mode"));
    }

    @Test
    void toQuery_omitsMode_whenBlankOrNull() {
        WtoHSPRequest r = new WtoHSPRequest();
        r.setIndicator("HS_P_0070");
        r.setReporter("702");
        r.setProductCode("100630");
        r.setPeriod("2018");
        r.setMode("  ");             // blank
        Map<String, String> q = r.toQuery();
        assertFalse(q.containsKey("mode"));

        r.setMode(null);             // null
        q = r.toQuery();
        assertFalse(q.containsKey("mode"));
    }
}
