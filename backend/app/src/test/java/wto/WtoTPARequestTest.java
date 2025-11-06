package wto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import wto.dto.WtoTPARequest;

class WtoTPARequestTest {

    @Test
    void toQuery_includesIndicatorReporterPeriodAndFormat() {
        WtoTPARequest r = new WtoTPARequest();
        r.setIndicator("TP_A_0170");
        r.setReporter("702");
        r.setPeriod("2015-2024");
        r.setFormat("json");

        Map<String, String> q = r.toQuery();
        assertEquals("TP_A_0170", q.get("i"));
        assertEquals("702", q.get("r"));
        assertEquals("2015-2024", q.get("ps"));
        assertEquals("json", q.get("fmt"));
    }
}
