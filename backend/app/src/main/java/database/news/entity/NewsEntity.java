package database.news.entity;

public class NewsEntity {

    private String newsLink;
    private String remarks;
    private boolean isHidden;

    // Getters and setters

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

    @Override
    public String toString() {
        return "NewsEntity{" +
                "newsLink='" + newsLink + '\'' +
                ", remarks='" + remarks + '\'' +
                ", isHidden=" + isHidden +
                '}';
    }
}