// WtoApiService.java
package wto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class WtoApiService {

    private final RestTemplate rest;

    @Value("${wto.api.key:#{environment.WTO_API_KEY}}")
    private String apiKey;

    private static final String BASE_URL = "https://api.wto.org/timeseries/v1/data"; // NOTE: https

    public WtoApiService(RestTemplateBuilder builder) {
        this.rest = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(45))
                .build();
    }

    public ResponseEntity<String> callWtoApi(Map<String, String> params) {
        try {
            UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(BASE_URL);
            params.forEach((k, v) -> { if (v != null && !v.isBlank()) b.queryParam(k, v); });
            if (apiKey != null && !apiKey.isBlank()) {
                b.queryParam("subscription-key", apiKey);
            }
            String url = b.toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            ResponseEntity<String> res = rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            return ResponseEntity.status(res.getStatusCode()).body(res.getBody());

        } catch (HttpStatusCodeException e) {
            // WTO returned a valid HTTP error (401/403/4xx/5xx) — forward as-is
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());

        } catch (Exception e) {
            // Network/SSL/DNS etc. Make sure we STILL return a response so nginx doesn't show its own 502 page.
            String msg = "Upstream WTO call failed: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"error\":\"" + msg + "\"}");
        }
    }
}
