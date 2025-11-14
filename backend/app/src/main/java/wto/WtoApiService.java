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
    private static final String BASE_URL = "https://api.wto.org/timeseries/v1/data";
    private static final String SUBSCRIPTION_KEY_PARAM = "subscription-key";
    private static final String API_KEY_MASK = "****";
    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int READ_TIMEOUT_SECONDS = 45;

    private final RestTemplate rest;

    @Value("${wto.api.key:#{environment.WTO_API_KEY}}")
    private String apiKey;

    public WtoApiService(RestTemplateBuilder builder) {
        this.rest = builder
                .setConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
                .build();
    }

    public ResponseEntity<String> callWtoApi(Map<String, String> params) {
        try {
            String url = buildUrl(params);
            String safeUrl = maskApiKey(url);
            log.info("Calling WTO: {}", safeUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            ResponseEntity<String> res = rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            log.info("WTO status: {}", res.getStatusCode().value());
            return ResponseEntity.status(res.getStatusCode()).body(res.getBody());

        } catch (HttpStatusCodeException e) {
            log.warn("WTO HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("WTO call failed", e);
            return buildErrorResponse(e);
        }
    }

    /**
     * Build URL with query parameters and API key
     */
    private String buildUrl(Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL);
        params.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                builder.queryParam(k, v);
            }
        });

        if (apiKey != null && !apiKey.isBlank()) {
            builder.queryParam(SUBSCRIPTION_KEY_PARAM, apiKey);
        } else {
            log.warn("WTO API key is not set. Set WTO_API_KEY env var or wto.api.key property.");
        }

        return builder.toUriString();
    }

    /**
     * Mask API key in URL for safe logging
     */
    private String maskApiKey(String url) {
        if (apiKey != null && !apiKey.isBlank()) {
            return url.replace(apiKey, API_KEY_MASK);
        }
        return url;
    }

    /**
     * Build error response with JSON format
     */
    private ResponseEntity<String> buildErrorResponse(Exception e) {
        String msg = "Upstream WTO call failed: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("{\"error\":\"" + msg + "\"}");
    }
}

