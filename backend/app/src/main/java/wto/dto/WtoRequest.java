package wto.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public interface WtoRequest {
    Map<String, String> toQuery();

    default Map<String, String> base() {
        return new LinkedHashMap<>();
    }
}