package wto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

/** TPA — TP_A_0160 / TP_A_0170 */
public class WtoTPARequest implements WtoRequest {

    @JsonProperty("i")   private String indicator; // required
    @JsonProperty("r")   private String reporter;  // required
    @JsonProperty("ps")  private String period;    // required
    @JsonProperty("fmt") private String format = "json";

    public String getIndicator() { return indicator; }
    public void setIndicator(String indicator) { this.indicator = indicator; }
    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    @Override public Map<String, String> toQuery() {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("i", indicator);
        q.put("r", reporter);
        q.put("ps", period);
        q.put("fmt", format);
        return q;
    }
}
