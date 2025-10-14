package wits;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import wits.dto.WitsTariffRateResponse;
import wits.exception.WitsApiException;
import wits.exception.WitsDataNotFoundException;

import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class WitsApiService {
    private static final Logger logger = LoggerFactory.getLogger(WitsApiService.class);
    
    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public WitsApiService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://wits.worldbank.org/API/V1/SDMX/V21")
                .build();
    }

    /**
     * Get tariff rate data from WITS for specific parameters
     * Returns structured data with MIN_RATE, MAX_RATE, AVG_RATE
     */
    public WitsTariffRateResponse getTariffRate(String reporter, String partner, 
                                               Integer product, String year) {
        try {
            logger.info("Fetching WITS data: reporter={}, partner={}, product={}, year={}",
                    reporter, partner, product, year);
            
            String path = String.format(
                "/datasource/TRN/reporter/%s/partner/%s/product/%s/year/%s/datatype/reported?format=JSON",
                reporter, partner, product, year
            );

            // Fetch data with proper error handling
            String body = webClient.get()
                .uri(path)
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return resp.bodyToMono(String.class).defaultIfEmpty("");
                    }
                    if (resp.statusCode().value() == 204 || resp.statusCode().is4xxClientError()) {
                        // WITS uses 204/404 when there's no data
                        return Mono.just("");
                    }
                    // For 5xx, bubble up as error
                    return resp.bodyToMono(String.class)
                               .flatMap(b -> Mono.error(new WitsApiException(
                                   "WITS API returned " + resp.statusCode(), 
                                   resp.statusCode().value()
                               )));
                })
                .block();

            // No data found
            if (body == null || body.isBlank()) {
                logger.warn("No data found in WITS");
                throw new WitsDataNotFoundException(reporter, partner, product, year);
            }

            // Extract all rates
            String minRate = extractObservationAttribute(body, "MIN_RATE");
            String maxRate = extractObservationAttribute(body, "MAX_RATE");
            String avgRate = extractObservationAttribute(body, "AVG_RATE");
            
            // If no rates extracted, data is not available
            if (minRate == null && maxRate == null && avgRate == null) {
                logger.warn("No tariff rates found in WITS response");
                throw new WitsDataNotFoundException(reporter, partner, product, year);
            }
            
            logger.info("Successfully retrieved WITS data: min={}, max={}, avg={}", 
                    minRate, maxRate, avgRate);

            return new WitsTariffRateResponse(
                reporter, partner, product, year,
                minRate, maxRate, avgRate
            );

        } catch (WitsDataNotFoundException e) {
            throw e; // Re-throw to be handled by exception handler
        } catch (WebClientResponseException e) {
            logger.error("WITS API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new WitsApiException("WITS API communication error", e.getStatusCode().value());
        } catch (Exception e) {
            logger.error("Unexpected error fetching WITS data", e);
            throw new WitsApiException("Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Get full raw JSON response from WITS (for advanced users)
     */
    public String getRawTariffData(String reporter, String partner, 
                                  Integer product, String year) {
        try {
            logger.info("Fetching raw WITS data: reporter={}, partner={}, product={}, year={}",
                    reporter, partner, product, year);
            
            String path = String.format(
                "/datasource/TRN/reporter/%s/partner/%s/product/%s/year/%s/datatype/reported?format=JSON",
                reporter, partner, product, year
            );

            String body = webClient.get()
                .uri(path)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (body == null || body.isBlank()) {
                throw new WitsDataNotFoundException(reporter, partner, product, year);
            }

            return body;

        } catch (WitsDataNotFoundException e) {
            throw e;
        } catch (WebClientResponseException e) {
            throw new WitsApiException("WITS API communication error", e.getStatusCode().value());
        } catch (Exception e) {
            throw new WitsApiException("Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Extract an observation attribute (e.g., MIN_RATE, MAX_RATE) from SDMX-JSON v2.1.
     * Logic:
     *  - Find position of the attribute in structure.attributes.observation (0-based)
     *  - Observation array layout: [measure, attr0, attr1, ...] => attribute position is (attrIndex + 1)
     *  - Read the attribute index from the observation array, then map it to the attribute's values[idx].id/name
     */
    private String extractObservationAttribute(String json, String attributeId) {
        try {
            JsonNode root = mapper.readTree(json);

            // 1) Locate the observation attributes in the structure and find attribute index (0-based)
            JsonNode obsAttrs = root.path("structure").path("attributes").path("observation");
            if (!obsAttrs.isArray()) return null;

            int attrIdx = -1; // 0-based within obsAttrs
            for (int i = 0; i < obsAttrs.size(); i++) {
                if (attributeId.equalsIgnoreCase(obsAttrs.get(i).path("id").asText())) {
                    attrIdx = i;
                    break;
                }
            }
            if (attrIdx < 0) return null;

            // 2) Get first series & first observation array
            JsonNode dataSets = root.path("dataSets");
            if (!dataSets.isArray() || dataSets.size() == 0) return null;

            JsonNode seriesNode = dataSets.get(0).path("series");
            if (!seriesNode.isObject()) return null;

            String firstSeriesKey = seriesNode.fieldNames().hasNext() ? seriesNode.fieldNames().next() : null;
            if (firstSeriesKey == null) return null;

            JsonNode observations = seriesNode.path(firstSeriesKey).path("observations");
            if (!observations.isObject()) return null;

            String firstObsKey = observations.fieldNames().hasNext() ? observations.fieldNames().next() : null;
            if (firstObsKey == null) return null;

            JsonNode obsArray = observations.path(firstObsKey);
            if (!obsArray.isArray() || obsArray.size() == 0) return null;

            // 3) Observation array layout: [value, attr0, attr1, ...]
            int obsPos = attrIdx + 1; // +1 because index 0 is the measure
            if (obsPos >= obsArray.size()) return null;

            JsonNode attrIndexNode = obsArray.get(obsPos);
            if (attrIndexNode == null || attrIndexNode.isNull()) return null;

            // If WITS uses coded indices into the attribute "values" array:
            if (attrIndexNode.isInt()) {
                int codeIdx = attrIndexNode.asInt();
                JsonNode values = obsAttrs.get(attrIdx).path("values");
                if (values.isArray() && codeIdx >= 0 && codeIdx < values.size()) {
                    JsonNode v = values.get(codeIdx);
                    // Prefer "id" (often numeric string), else "name"
                    String out = v.path("id").asText(null);
                    if (out == null || out.isBlank()) out = v.path("name").asText(null);
                    return out;
                }
            }

            // Fallback: sometimes the value itself is put directly
            if (attrIndexNode.isNumber() || attrIndexNode.isTextual()) {
                return attrIndexNode.asText();
            }

            return null;

        } catch (Exception e) {
            logger.error("Error extracting attribute {}: {}", attributeId, e.getMessage());
            return null;
        }
    }
}