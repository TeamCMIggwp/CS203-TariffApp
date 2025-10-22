// WtoApiService.java
package wto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class WtoApiService {

    private static final Logger log = LoggerFactory.getLogger(WtoApiService.class);
    private final RestTemplate rest;

    @Value("${wto.api.key:#{environment.WTO_API_KEY}}")
    private String apiKey;

    private static final String BASE_URL = "https://api.wto.org/timeseries/v1/data";

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
            } else {
                log.warn("WTO API key is not set. Set WTO_API_KEY env var or wto.api.key property.");
            }
            String url = b.toUriString();

            String safeUrl = url;
            if (apiKey != null && !apiKey.isBlank()) {
                safeUrl = url.replace(apiKey, "****");
            }
            log.info("Calling WTO: {}", safeUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            ResponseEntity<String> res = rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            log.info("WTO status: {}", res.getStatusCodeValue());
            return ResponseEntity.status(res.getStatusCode()).body(res.getBody());

        } catch (HttpStatusCodeException e) {
            log.warn("WTO HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("WTO call failed", e);
            // Return a JSON error so proxies don't 502 with HTML
            String msg = "Upstream WTO call failed: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body("{\"error\":\"" + msg + "\"}");

        }
    }
}

