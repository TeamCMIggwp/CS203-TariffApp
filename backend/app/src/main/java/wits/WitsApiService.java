package wits;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class WitsApiService {

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

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

    /** Hardcoded demo that returns MIN_RATE only (as plain text). */
    public ResponseEntity<String> getMinRateOnly() {
        String path = "/datasource/TRN/reporter/840/partner/000/product/100630/year/2020/datatype/reported?format=JSON";
        String body = webClient.get().uri(path).retrieve().bodyToMono(String.class).block();
        String minRate = extractObservationAttribute(body, "MIN_RATE");
        return (minRate != null) ? ResponseEntity.ok(minRate) : ResponseEntity.noContent().build();
    }

    /** Parameterized version that returns MIN_RATE only (as plain text). */
    public ResponseEntity<String> getMinRateOnly(String reporter, String partner, String product, String year) {
    try {
        String path = String.format(
                "/datasource/TRN/reporter/%s/partner/%s/product/%s/year/%s/datatype/reported?format=JSON",
                reporter, partner, product, year
        );
        String body = webClient.get()
                .uri(path)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        String minRate = extractObservationAttribute(body, "MIN_RATE");

        if (minRate == null) {
            return ResponseEntity.ok("No result found in WITs");
        }

        return ResponseEntity.ok(minRate);

    } catch (Exception e) {
        // If any parsing or API errors happen, still return plain text
        return ResponseEntity.status(404).body("API Error");
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
            return null;
        }
    }
}