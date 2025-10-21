package wto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
public class WtoApiService {
    private final RestTemplate rest = new RestTemplate();

    @Value("${wto.api.key:#{environment.WTO_API_KEY}}")
    private String apiKey;

    private static final String BASE_URL = "https://api.wto.org/timeseries/v1/data";

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
            return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }
}
