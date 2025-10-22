package wto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

/** HSP — e.g., HS_P_0070 */
public class WtoHSPRequest implements WtoRequest {

    @JsonProperty("i")   private String indicator;    // required
    @JsonProperty("r")   private String reporter;     // required
    @JsonProperty("p")   private String partner;      // optional
    @JsonProperty("pc")  private String productCode;  // required
    @JsonProperty("ps")  private String period;       // required
    @JsonProperty("fmt") private String format = "json";
    @JsonProperty("mode")private String mode; // optional

    public String getIndicator() { return indicator; }
    public void setIndicator(String indicator) { this.indicator = indicator; }
    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }
    public String getPartner() { return partner; }
    public void setPartner(String partner) { this.partner = partner; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    @Override public Map<String, String> toQuery() {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("i", indicator);
        q.put("r", reporter);
        q.put("p", partner);
        q.put("pc", productCode);
        q.put("ps", period);
        q.put("fmt", format);
        if (mode != null && !mode.isBlank()) q.put("mode", mode);
        return q;
    }
}
