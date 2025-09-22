package com.example.app.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class WitsApiService {

    private final WebClient webClient;

    public WitsApiService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://wits.worldbank.org/API/V1/SDMX/V21")
                .build();
    }

    /**
     * Hardcoded example: US (840), World (000), Rice (100630), Year=2020, datatype=reported
     */
    public ResponseEntity<String> getTariffData() {
        String path = "/datasource/TRN/reporter/840/partner/000/product/100630/year/2020/datatype/reported?format=JSON";

        String body = webClient.get()
                .uri(path)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return ResponseEntity.ok(body);
    }
}
