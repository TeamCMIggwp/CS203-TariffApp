package wto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.mockito.ArgumentCaptor;

/**
 * Pure unit tests for WtoController — no Spring context, no MockMvc.
 * Mirrors ScraperControllerTest style: direct method invocation.
 */
@ExtendWith(MockitoExtension.class)
class WtoControllerTest {

    @Mock
    private WtoApiService svc;

    @InjectMocks
    private WtoController controller;

    private ResponseEntity<String> ok(String body) {
        return ResponseEntity.ok(body);
    }

    @BeforeEach
    void setup() {
        when(svc.callWtoApi(anyMap())).thenReturn(ok("{\"ok\":true}"));
    }

    // ---------- Echo bypass path ----------
    @Test
    void echo_true_returnsEchoOfInputs() {
        ResponseEntity<?> res = controller.getObservations(
                "hs_p_0070", // indicator (lower -> upper in controller)
                "840",       // r
                "156",       // p
                "100630",    // pc
                "2018-2024", // ps
                "json",      // fmt
                "compact",   // mode
                true         // echo
        );
        assertEquals(200, res.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) res.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("echo"));
        Map<String, Object> echo = (Map<String, Object>) body.get("echo");
        assertEquals("HS_P_0070", echo.get("indicator"));
        assertEquals("840", echo.get("r"));
        assertEquals("156", echo.get("p"));
        assertEquals("100630", echo.get("pc"));
        assertEquals("2018-2024", echo.get("ps"));
        assertEquals("json", echo.get("fmt"));
        assertEquals("compact", echo.get("mode"));
        verifyNoInteractions(svc);
    }

    // ---------- TPA group (TP_A_0160 / TP_A_0170) ----------
    @Test
    void tpa_missingRequiredParams_returns422() {
        ResponseEntity<?> res = controller.getObservations(
                "tp_a_0160",
                null,      // r missing
                null,      // p not used
                null,      // pc not used
                null,      // ps missing
                "json",
                null,      // mode not used
                false
        );
        assertEquals(422, res.getStatusCodeValue());
        assertTrue(res.getBody().toString().contains("Required params for TP_A_0160"));
        verifyNoInteractions(svc);
    }

    @Test
    void tpa_success_callsServiceWithQuery() {
        ResponseEntity<?> res = controller.getObservations(
                "tp_a_0170",
                "702",     // r
                null,
                null,
                "2015-2024", // ps
                "json",
                null,
                false
        );
        assertEquals(200, res.getStatusCodeValue());
        ArgumentCaptor<Map<String, String>> cap = ArgumentCaptor.forClass(Map.class);
        verify(svc).callWtoApi(cap.capture());
        Map<String, String> q = cap.getValue();
        assertEquals("TP_A_0170", q.get("i"));
        assertEquals("702", q.get("r"));
        assertEquals("2015-2024", q.get("ps"));
        assertEquals("json", q.get("fmt"));
    }

    // ---------- TPB group (TP_B_0180 / TP_B_0190) ----------
    @Test
    void tpb_missingReporter_returns422() {
        ResponseEntity<?> res = controller.getObservations(
                "TP_B_0180",
                null,   // r missing
                null,
                null,
                null,
                "json",
                null,
                false
        );
        assertEquals(422, res.getStatusCodeValue());
        assertTrue(res.getBody().toString().contains("Required param for TP_B_0180: r"));
        verifyNoInteractions(svc);
    }

    @Test
    void tpb_success_defaultsModeToFull_whenNull() {
        ResponseEntity<?> res = controller.getObservations(
                "tp_b_0190",
                "840",
                null,
                null,
                null,
                "json",
                null,     // mode null -> controller should default to "full"
                false
        );
        assertEquals(200, res.getStatusCodeValue());
        ArgumentCaptor<Map<String, String>> cap = ArgumentCaptor.forClass(Map.class);
        verify(svc).callWtoApi(cap.capture());
        Map<String, String> q = cap.getValue();
        assertEquals("TP_B_0190", q.get("i"));
        assertEquals("840", q.get("r"));
        assertEquals("json", q.get("fmt"));
        assertEquals("full", q.get("mode")); // default applied
    }

    // ---------- HSP default group (e.g., HS_P_0070) ----------
    @Test
    void hsp_missingAnyRequired_returns422() {
        ResponseEntity<?> res = controller.getObservations(
                "HS_P_0070",
                "702",     // r present
                "840",     // p optional
                null,      // pc missing
                "2018",    // ps present
                "json",
                null,
                false
        );
        assertEquals(422, res.getStatusCodeValue());
        assertTrue(res.getBody().toString().contains("Required params for HS_P_0070 (HSP): r, pc, ps"));
        verifyNoMoreInteractions(svc);
    }

    @Test
    void hsp_success_includesOptionalPartner_andOptionalModeWhenProvided() {
        ResponseEntity<?> res = controller.getObservations(
                "HS_P_0070",
                "702",        // r
                "840",        // p optional
                "100630",     // pc
                "2018-2020",  // ps
                "json",
                "compact",    // mode present (optional)
                false
        );
        assertEquals(200, res.getStatusCodeValue());
        ArgumentCaptor<Map<String, String>> cap = ArgumentCaptor.forClass(Map.class);
        verify(svc).callWtoApi(cap.capture());
        Map<String, String> q = cap.getValue();
        assertEquals("HS_P_0070", q.get("i"));
        assertEquals("702", q.get("r"));
        assertEquals("840", q.get("p"));
        assertEquals("100630", q.get("pc"));
        assertEquals("2018-2020", q.get("ps"));
        assertEquals("json", q.get("fmt"));
        assertEquals("compact", q.get("mode")); // only set when provided
    }
}
