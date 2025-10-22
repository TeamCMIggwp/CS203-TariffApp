package wto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

/** TPB — TP_B_0180 / TP_B_0190 */
public class WtoTPBRequest implements WtoRequest {

    @JsonProperty("i")   private String indicator; // required
    @JsonProperty("r")   private String reporter;  // required
    @JsonProperty("fmt") private String format = "json";
    @JsonProperty("mode")private String mode = "full"; // required by your spec

    public String getIndicator() { return indicator; }
    public void setIndicator(String indicator) { this.indicator = indicator; }
    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    @Override public Map<String, String> toQuery() {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("i", indicator);
        q.put("r", reporter);
        q.put("fmt", format);
        q.put("mode", mode);
        return q;
    }
}
