package database.news.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class NewsResponse {

    @JsonProperty("newsLink")
    private String newsLink;

    @JsonProperty("remarks")
    private String remarks;

    @JsonProperty("isHidden")
    private boolean isHidden;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    public NewsResponse(String newsLink, String remarks, boolean isHidden) {
        this.newsLink = newsLink;
        this.remarks = remarks;
        this.isHidden = isHidden;
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

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}