package database.userhiddensources.entity;

import java.time.LocalDateTime;

public class UserHiddenSourcesEntity {

    private Integer id;
    private String userEmail;
    private String newsLink;
    private LocalDateTime hiddenAt;

    // Getters and setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

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

    @Override
    public String toString() {
        return "UserHiddenSourcesEntity{" +
                "id=" + id +
                ", userEmail='" + userEmail + '\'' +
                ", newsLink='" + newsLink + '\'' +
                ", hiddenAt=" + hiddenAt +
                '}';
    }
}
