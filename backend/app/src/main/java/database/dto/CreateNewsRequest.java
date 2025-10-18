package database.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateNewsRequest {
    
    @NotBlank(message = "News link is required")
    @Size(max = 200, message = "News link must not exceed 200 characters")
    @JsonProperty("newsLink")
    private String newsLink;
    
    @Size(max = 100, message = "Remarks must not exceed 100 characters")
    @JsonProperty("remarks")
    private String remarks;
    
    // Getters and Setters
    public String getNewsLink() { 
        return newsLink; 
    }
    
    public void setNewsLink(String newsLink) { 
        this.newsLink = newsLink; 
    }
    
    public String getRemarks() { 
        return remarks; 
    }
    
    public void setRemarks(String remarks) { 
        this.remarks = remarks; 
    }
    
    @Override
    public String toString() {
        return "CreateNewsRequest{" +
                "newsLink='" + newsLink + '\'' +
                ", remarks='" + remarks + '\'' +
                '}';
    }
}