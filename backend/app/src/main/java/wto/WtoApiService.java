package wto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class WtoApiService {
    private final RestTemplate rest = new RestTemplate();

    @Value("${wto.api.key}")
    private String apiKey;

    public ResponseEntity<String> getAllIndicators() {
        String url = "https://api.wto.org/timeseries/v1/data?i=HS_P_0070&r=all&p=default&ps=default&pc=100610,100620,100630,100640&spc=false&fmt=json&mode=full&dec=default&off=0&max=500&head=H&lang=1&meta=false";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Ocp-Apim-Subscription-Key", apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            return rest.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }
}