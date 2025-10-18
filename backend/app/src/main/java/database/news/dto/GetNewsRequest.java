package database.news.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetNewsRequest {
    
    @Size(max = 200, message = "News link must not exceed 200 characters")
    @JsonProperty("newsLink")
    private String newsLink;
    
    // Getters and Setters
    public String getNewsLink() { 
        return newsLink; 
    }
    
    public void setNewsLink(String newsLink) { 
        this.newsLink = newsLink; 
    }
    
    @Override
    public String toString() {
        return "GetNewsRequest{" +
                "newsLink='" + newsLink + '\'' +
                '}';
    }
}