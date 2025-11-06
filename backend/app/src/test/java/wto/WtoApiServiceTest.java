package wto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.mockito.ArgumentCaptor;
import java.util.*;

/**
 * Unit tests for WtoApiService — no Spring context.
 * Injects private fields (apiKey, RestTemplate) via ReflectionTestUtils.
 */
class WtoApiServiceTest {

    private WtoApiService service;

    @Mock
    private RestTemplate rest;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new WtoApiService(new RestTemplateBuilder());
        // Inject mock rest and a known API key
        ReflectionTestUtils.setField(service, "rest", rest);
        ReflectionTestUtils.setField(service, "apiKey", "TESTKEY");
    }

    @Test
    void success_returnsUpstreamStatusAndBody_andIncludesApiKeyInUrl() {
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("{\"ok\":true}"));

        ResponseEntity<String> res = service.callWtoApi(Map.of(
                "i", "HS_P_0070",
                "r", "702",
                "ps", "2018",
                "pc", "100630",
                "fmt", "json"
        ));

        assertEquals(200, res.getStatusCodeValue());
        assertEquals("{\"ok\":true}", res.getBody());

        // Capture the URL to assert query params added
        ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
        verify(rest).exchange(urlCap.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
        String url = urlCap.getValue();
        assertTrue(url.contains("i=HS_P_0070"));
        assertTrue(url.contains("r=702"));
        assertTrue(url.contains("ps=2018"));
        assertTrue(url.contains("pc=100630"));
        assertTrue(url.contains("fmt=json"));
        assertTrue(url.contains("subscription-key=TESTKEY")); // key is appended when present
    }

    @Test
    void httpError_isPropagatedWithSameStatusAndBody() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                "{\"error\":\"bad\"}".getBytes(), null);
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
            .thenThrow(ex);

        ResponseEntity<String> res = service.callWtoApi(Map.of("i", "TP_A_0160", "r", "702", "ps", "2020"));
        assertEquals(400, res.getStatusCodeValue());
        assertEquals("{\"error\":\"bad\"}", res.getBody());
    }

    @Test
    void genericException_returns502WithJsonError() {
        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new RuntimeException("boom"));

        ResponseEntity<String> res = service.callWtoApi(Map.of("i", "TP_B_0180", "r", "702"));
        assertEquals(502, res.getStatusCodeValue());
        assertNotNull(res.getBody());
        assertTrue(res.getBody().contains("Upstream WTO call failed"));
        assertTrue(res.getBody().contains("RuntimeException"));
    }

    @Test
    void noApiKey_doesNotAppendSubscriptionKeyParam() {
        ReflectionTestUtils.setField(service, "apiKey", ""); // blank -> not appended

        when(rest.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("{}"));

        ResponseEntity<String> res = service.callWtoApi(Map.of("i", "HS_P_0070", "r", "702", "ps", "2018", "pc", "100630"));
        assertEquals(200, res.getStatusCodeValue());

        ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
        verify(rest).exchange(urlCap.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
        String url = urlCap.getValue();
        assertFalse(url.contains("subscription-key="));
    }
}
