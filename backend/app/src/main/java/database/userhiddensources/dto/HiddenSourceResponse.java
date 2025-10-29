package database.userhiddensources.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class HiddenSourceResponse {

    @JsonProperty("newsLink")
    private String newsLink;

    @JsonProperty("hiddenAt")
    private LocalDateTime hiddenAt;

    public HiddenSourceResponse(String newsLink, LocalDateTime hiddenAt) {
        this.newsLink = newsLink;
        this.hiddenAt = hiddenAt;
    }

    // Getters and Setters
    public String getNewsLink() {
        return newsLink;
    }

    public void setNewsLink(String newsLink) {
        this.newsLink = newsLink;
    }

    public LocalDateTime getHiddenAt() {
        return hiddenAt;
    }

    public void setHiddenAt(LocalDateTime hiddenAt) {
        this.hiddenAt = hiddenAt;
    }
}
