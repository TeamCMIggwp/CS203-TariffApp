package geminianalysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisResponse {
    
    @JsonProperty("id")
    private String id; // Could be UUID for tracking
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("analysisType")
    private String analysisType; // "structured" or "text"
    
    @JsonProperty("analysis")
    private Object analysis; // JsonNode or String
    
    @JsonProperty("summary")
    private String summary;
    
    @JsonProperty("confidence")
    private String confidence;
    
    @JsonProperty("error")
    private String error;
    
    // Constructor for success
    public AnalysisResponse(boolean success, String analysisType, Object analysis, 
                           String summary, String confidence) {
        this.success = success;
        this.timestamp = LocalDateTime.now();
        this.analysisType = analysisType;
        this.analysis = analysis;
        this.summary = summary;
        this.confidence = confidence;
    }
    
    // Constructor for error
    public AnalysisResponse(boolean success, String error) {
        this.success = success;
        this.timestamp = LocalDateTime.now();
        this.error = error;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getAnalysisType() { return analysisType; }
    public void setAnalysisType(String analysisType) { this.analysisType = analysisType; }
    
    public Object getAnalysis() { return analysis; }
    public void setAnalysis(Object analysis) { this.analysis = analysis; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}