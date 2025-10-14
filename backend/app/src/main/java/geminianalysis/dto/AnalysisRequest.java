package geminianalysis.dto;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AnalysisRequest {
    
    @NotBlank(message = "Data to analyze is required")
    @JsonProperty("data")
    private String data;
    
    @JsonProperty("prompt")
    private String prompt;
    
    // Constructors
    public AnalysisRequest() {}
    
    public AnalysisRequest(String data, String prompt) {
        this.data = data;
        this.prompt = prompt;
    }
    
    // Getters and Setters
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}