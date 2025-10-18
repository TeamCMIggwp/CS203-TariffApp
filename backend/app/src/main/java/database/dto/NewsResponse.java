package database.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class NewsResponse {
    
    @JsonProperty("newsLink")
    private String newsLink;
    
    @JsonProperty("remarks")
    private String remarks;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    public NewsResponse(String newsLink, String remarks) {
        this.newsLink = newsLink;
        this.remarks = remarks;
        this.timestamp = LocalDateTime.now();
    }
    
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
    
    public LocalDateTime getTimestamp() { 
        return timestamp; 
    }
    
    public void setTimestamp(LocalDateTime timestamp) { 
        this.timestamp = timestamp; 
    }
}