package com.example.app.service;

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
    String url = "http://api.wto.org/timeseries/v1/territory/regions?1";

    HttpHeaders headers = new HttpHeaders();
    headers.set("Ocp-Apim-Subscription-Key", apiKey); // WTO header name
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      return rest.exchange(url, HttpMethod.GET, entity, String.class);
    } catch (HttpStatusCodeException ex) {
      // propagate real status + body instead of crashing with 500
      return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
    }
  }
}



